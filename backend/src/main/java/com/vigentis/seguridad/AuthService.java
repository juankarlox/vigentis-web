package com.vigentis.seguridad;

import com.vigentis.comun.UsuarioActual;
import com.vigentis.empresas.Empresa;
import com.vigentis.empresas.EmpresaRepository;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import com.vigentis.riesgos.CriteriosRiesgo;
import com.vigentis.riesgos.CriteriosRiesgoRepository;
import com.vigentis.riesgos.CriteriosService;
import com.vigentis.usuarios.Permiso;
import com.vigentis.usuarios.PermisoRepository;
import com.vigentis.usuarios.Rol;
import com.vigentis.usuarios.RolRepository;
import com.vigentis.usuarios.Usuario;
import com.vigentis.usuarios.UsuarioRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    public static final int MAXIMO_INTENTOS = 5;
    public static final int MINUTOS_BLOQUEO = 15;

    /** Lo que recibe la web al iniciar sesión o registrarse. */
    public record Sesion(String token, Instant expira, Perfil usuario) {}

    /** Datos del usuario con sesión (sin nada sensible). */
    public record Perfil(Long id, String nombre, String correo, String rol, boolean administrador,
                         Long empresaId, String empresa, List<String> permisos) {}

    /** Datos del formulario "Registrar mi empresa". */
    public record Registro(String empresa, String nit, String sector, String nombre, String correo, String clave) {}

    // Roles con los que arranca cada empresa nueva (la empresa los puede cambiar después)
    private static final List<String> PERMISOS_ANALISTA = List.of(
            "ACTIVOS_VER", "RIESGOS_VER", "RIESGOS_GESTIONAR", "CONTROLES_VER",
            "INCIDENTES_VER", "INCIDENTES_GESTIONAR", "DOCUMENTOS_VER", "REPORTES_VER");
    private static final List<String> PERMISOS_RESPONSABLE_SGSI = List.of(
            "ACTIVOS_VER", "RIESGOS_VER", "RIESGOS_ACEPTAR", "CRITERIOS_GESTIONAR", "CONTROLES_VER",
            "CONTROLES_GESTIONAR", "INCIDENTES_VER", "AUDITORIAS_VER", "AUDITORIAS_IMPORTAR", "DOCUMENTOS_VER",
            "DOCUMENTOS_GESTIONAR", "DOCUMENTOS_APROBAR", "REPORTES_VER", "HISTORIAL_VER");

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PermisoRepository permisos;
    private final EmpresaRepository empresas;
    private final CriteriosRiesgoRepository criterios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwt;
    private final HistorialService historial;
    private final JdbcTemplate jdbc;
    private final long horasSesion;
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, RolRepository roles, PermisoRepository permisos,
                       EmpresaRepository empresas, CriteriosRiesgoRepository criterios, PasswordEncoder encoder,
                       JwtEncoder jwt, HistorialService historial, JdbcTemplate jdbc,
                       @Value("${vigentis.jwt.horas:8}") long horasSesion) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.permisos = permisos;
        this.empresas = empresas;
        this.criterios = criterios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.historial = historial;
        this.jdbc = jdbc;
        this.horasSesion = horasSesion;
        // Se usa para tardar lo mismo cuando el correo no existe (no revelar qué correos existen)
        this.hashFalso = encoder.encode("vigentis-clave-que-no-existe-123");
    }

    // ================= INICIAR SESIÓN =================

    // noRollbackFor: aunque el login falle, el intento fallido SÍ debe quedar guardado
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Sesion login(String correo, String clave) {
        // Sin sesión todavía: la búsqueda por correo es en toda la plataforma (el correo es único)
        Usuario u = usuarios.findByCorreoIgnoreCase(correo.trim()).orElse(null);
        if (u == null) {
            encoder.matches(clave, hashFalso);
            throw credencialesInvalidas();
        }
        if (u.isBloqueado()) {
            throw cuentaBloqueada(u);
        }
        if (!encoder.matches(clave, u.hashClave())) {
            u.registrarFallo(MAXIMO_INTENTOS, MINUTOS_BLOQUEO);
            usuarios.saveAndFlush(u);
            if (u.isBloqueado()) {
                historial.registrarComo(u.empresa(), u.getId(), "USUARIO", u.getId(), null, Historial.Accion.EDITAR,
                        "Cuenta bloqueada por " + MAXIMO_INTENTOS + " intentos fallidos: " + u.getCorreo());
                throw cuentaBloqueada(u);
            }
            throw credencialesInvalidas();
        }
        if (!u.getActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu usuario está desactivado. Contacta al administrador de tu empresa");
        }
        u.registrarAccesoExitoso();
        usuarios.saveAndFlush(u);
        historial.registrarComo(u.empresa(), u.getId(), "USUARIO", u.getId(), null, Historial.Accion.INICIAR_SESION,
                "Inicio de sesión: " + u.getCorreo());
        return crearSesion(u);
    }

    // ================= REGISTRAR UNA EMPRESA NUEVA =================

    /**
     * Crea el espacio de una empresa: la empresa, sus 4 roles de ejemplo, sus criterios de riesgo,
     * los 93 controles del Anexo A y la cuenta de quien la registra como administrador.
     */
    @Transactional
    public Sesion registrar(Registro d) {
        String correo = d.correo().trim().toLowerCase(Locale.ROOT);
        if (correoEnUso(correo, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        ReglasClave.validar(d.clave());

        Empresa e = new Empresa();
        e.setNombre(d.empresa().trim());
        e.setNit(limpio(d.nit()));
        e.setSector(limpio(d.sector()));
        empresas.saveAndFlush(e);
        Long eid = e.getId();

        // Roles de ejemplo
        List<Permiso> todos = permisos.findAll();
        Rol admin = crearRol(eid, "Administrador de TI", "Acceso total, incluida la administración de usuarios y roles",
                new HashSet<>(todos), true);
        crearRol(eid, "Analista de ciberseguridad", "Evalúa riesgos y atiende incidentes",
                new HashSet<>(permisos.findByCodigoIn(PERMISOS_ANALISTA)), false);
        crearRol(eid, "Responsable del SGSI", "Gestiona controles, documentos, auditorías y aprueba riesgos",
                new HashSet<>(permisos.findByCodigoIn(PERMISOS_RESPONSABLE_SGSI)), false);
        crearRol(eid, "Auditor", "Solo consulta, con acceso al historial",
                new HashSet<>(todos.stream().filter(p -> p.getCodigo().endsWith("_VER")).toList()), false);

        // Criterios de riesgo por defecto
        CriteriosRiesgo c = new CriteriosRiesgo();
        c.asignarEmpresa(eid);
        c.setUmbralMedio(5);
        c.setUmbralAlto(10);
        c.setUmbralCritico(20);
        c.setNivelAceptable(4);
        c.setMesesRevision(6);
        c.setEscalaProbabilidad(CriteriosService.ESCALA_PROBABILIDAD);
        c.setEscalaImpacto(CriteriosService.ESCALA_IMPACTO);
        criterios.save(c);

        // Los 93 controles del Anexo A, sin evaluar
        jdbc.update("INSERT INTO controles (empresa_id, codigo_iso, nombre, tema) "
                + "SELECT ?, codigo_iso, nombre, tema FROM catalogo_controles ORDER BY orden", eid);

        // Quien registra la empresa queda como administrador
        Usuario u = new Usuario();
        u.asignarEmpresa(eid);
        u.setNombre(d.nombre().trim());
        u.setCorreo(correo);
        u.setRol(admin);
        u.setActivo(true);
        u.asignarHashClave(encoder.encode(d.clave()));
        u.registrarAccesoExitoso();
        usuarios.saveAndFlush(u);

        historial.registrarComo(eid, u.getId(), "EMPRESA", eid, null, Historial.Accion.CREAR,
                "Empresa registrada: " + e.getNombre() + " (administrador " + correo + ")");
        return crearSesion(u, e);
    }

    // ================= MI CUENTA =================

    @Transactional(readOnly = true)
    public Perfil yo() {
        Usuario u = usuarioActual();
        return perfil(u, empresas.findById(u.empresa()).orElse(null));
    }

    @Transactional
    public void cambiarClave(String actual, String nueva) {
        Usuario u = usuarioActual();
        if (!encoder.matches(actual, u.hashClave())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente a la actual");
        }
        ReglasClave.validar(nueva);
        u.asignarHashClave(encoder.encode(nueva));
        usuarios.saveAndFlush(u);
        historial.registrar("USUARIO", u.getId(), null, Historial.Accion.EDITAR,
                "Cambio de contraseña: " + u.getCorreo());
    }

    /** true si el correo ya lo usa otra cuenta en CUALQUIER empresa. */
    public boolean correoEnUso(String correo, Long excepto) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM usuarios WHERE LOWER(correo) = LOWER(?)", Long.class, correo);
        return ids.stream().anyMatch(id -> !id.equals(excepto));
    }

    // ================= AYUDAS =================

    private Sesion crearSesion(Usuario u) {
        return crearSesion(u, empresas.findById(u.empresa()).orElse(null));
    }

    private Sesion crearSesion(Usuario u, Empresa e) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(horasSesion, ChronoUnit.HOURS);
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("vigentis")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(u.getId().toString())
                .claim("empresa", u.empresa())
                .claim("nombre", u.getNombre())
                .build();
        String token = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
                .getTokenValue();
        return new Sesion(token, expira, perfil(u, e));
    }

    private static Perfil perfil(Usuario u, Empresa e) {
        List<String> codigos = u.getRol().getPermisos().stream().map(Permiso::getCodigo).sorted().toList();
        return new Perfil(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().getNombre(),
                u.esAdministradorSistema(), u.empresa(), e == null ? null : e.getNombre(), codigos);
    }

    private Rol crearRol(Long empresaId, String nombre, String descripcion, Set<Permiso> lista, boolean sistema) {
        Rol r = new Rol();
        r.asignarEmpresa(empresaId);
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        r.setPermisos(lista);
        if (sistema) r.marcarComoSistema();
        return roles.save(r);
    }

    private Usuario usuarioActual() {
        Long id = UsuarioActual.id();
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        return usuarios.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión"));
    }

    private static String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }

    private static ResponseStatusException cuentaBloqueada(Usuario u) {
        String hora = u.getBloqueadoHasta().format(DateTimeFormatter.ofPattern("hh:mm a"));
        return new ResponseStatusException(HttpStatus.LOCKED,
                "Cuenta bloqueada por intentos fallidos. Intenta de nuevo después de las " + hora);
    }
}

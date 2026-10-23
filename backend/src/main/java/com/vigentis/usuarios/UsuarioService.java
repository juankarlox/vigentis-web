package com.vigentis.usuarios;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.comun.Textos;
import com.vigentis.comun.UsuarioActual;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import com.vigentis.seguridad.AuthService;
import com.vigentis.seguridad.ReglasClave;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    /** Para listas desplegables de "responsable". */
    public record Opcion(Long id, String nombre) {}

    private final UsuarioRepository repo;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final HistorialService historial;
    private final AuthService auth;

    public UsuarioService(UsuarioRepository repo, RolRepository roles, PasswordEncoder encoder,
                          HistorialService historial, AuthService auth) {
        this.auth = auth;
        this.repo = repo;
        this.roles = roles;
        this.encoder = encoder;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Opcion> opciones() {
        return repo.findByActivoTrueOrderByNombreAsc().stream()
                .map(u -> new Opcion(u.getId(), u.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario con id " + id));
    }

    @Transactional
    public Usuario crear(UsuarioRequest d) {
        String correo = d.correo().trim().toLowerCase(Locale.ROOT);
        if (auth.correoEnUso(correo, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        if (Textos.limpio(d.clave()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria al crear el usuario");
        }
        ReglasClave.validar(d.clave());
        Usuario u = new Usuario();
        u.setNombre(d.nombre().trim());
        u.setCorreo(correo);
        u.setRol(rol(d.rolId()));
        u.setActivo(d.activo() == null || d.activo());
        u.asignarHashClave(encoder.encode(d.clave()));
        repo.saveAndFlush(u);
        historial.registrar("USUARIO", u.getId(), null, Historial.Accion.CREAR,
                "Usuario creado: " + u.getCorreo() + " con rol " + u.getRol().getNombre());
        return u;
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioRequest d) {
        Usuario u = obtener(id);
        String correo = d.correo().trim().toLowerCase(Locale.ROOT);
        if (auth.correoEnUso(correo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        boolean activo = d.activo() == null ? u.getActivo() : d.activo();
        if (Objects.equals(UsuarioActual.id(), id) && !activo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propio usuario");
        }
        boolean eraAdmin = u.getActivo() && u.esAdministradorSistema();
        u.setNombre(d.nombre().trim());
        u.setCorreo(correo);
        u.setRol(rol(d.rolId()));
        u.setActivo(activo);
        boolean cambioClave = Textos.limpio(d.clave()) != null;
        if (cambioClave) {
            ReglasClave.validar(d.clave());
            u.asignarHashClave(encoder.encode(d.clave()));
            u.desbloquear();
        }
        repo.saveAndFlush(u);
        if (eraAdmin && repo.contarAdministradoresActivos() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe quedar al menos un administrador activo");
        }
        historial.registrar("USUARIO", u.getId(), null, Historial.Accion.EDITAR,
                "Usuario actualizado: " + u.getCorreo() + " (rol " + u.getRol().getNombre()
                + (u.getActivo() ? ", activo" : ", desactivado") + (cambioClave ? ", contraseña restablecida" : "") + ")");
        return u;
    }

    @Transactional
    public Usuario desbloquear(Long id) {
        Usuario u = obtener(id);
        u.desbloquear();
        repo.saveAndFlush(u);
        historial.registrar("USUARIO", u.getId(), null, Historial.Accion.EDITAR, "Usuario desbloqueado: " + u.getCorreo());
        return u;
    }

    private Rol rol(Long rolId) {
        return roles.findById(rolId).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el rol con id " + rolId));
    }
}

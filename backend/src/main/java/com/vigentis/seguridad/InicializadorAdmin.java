package com.vigentis.seguridad;

import com.vigentis.usuarios.Rol;
import com.vigentis.usuarios.RolRepository;
import com.vigentis.usuarios.Usuario;
import com.vigentis.usuarios.UsuarioRepository;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * La primera vez que arranca Vigentis (sin usuarios), crea el administrador inicial.
 * La contraseña se toma de VIGENTIS_ADMIN_CLAVE o, si no existe, se genera y se muestra UNA vez en la consola.
 */
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);
    private static final long EMPRESA_DEMO = 1L;
    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;
    private final String claveConfigurada;
    private final String nombre;
    private final String correo;

    public InicializadorAdmin(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder,
                              @Value("${VIGENTIS_ADMIN_CLAVE:}") String claveConfigurada,
                              @Value("${VIGENTIS_ADMIN_NOMBRE:Administrador}") String nombre,
                              @Value("${VIGENTIS_ADMIN_CORREO:admin@vigentis.local}") String correo) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
        this.claveConfigurada = claveConfigurada;
        this.nombre = nombre.isBlank() ? "Administrador" : nombre.trim();
        this.correo = correo.isBlank() ? "admin@vigentis.local" : correo.trim().toLowerCase();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;

        // El administrador inicial pertenece a la "Empresa demo" (id 1), creada por la migración V6
        Rol admin = roles.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getEsSistema()) && Long.valueOf(EMPRESA_DEMO).equals(r.empresa()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe el rol de administrador de la Empresa demo"));

        boolean generada = claveConfigurada == null || claveConfigurada.isBlank();
        String clave = generada ? generarClave() : claveConfigurada;

        Usuario u = new Usuario();
        u.asignarEmpresa(EMPRESA_DEMO);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setRol(admin);
        u.setActivo(true);
        u.asignarHashClave(encoder.encode(clave));
        usuarios.save(u);

        if (generada) {
            log.warn("""

                ============================================================
                  VIGENTIS - ADMINISTRADOR INICIAL CREADO
                  Correo:     {}
                  Contraseña: {}
                  Anótala y cámbiala después de iniciar sesión.
                  (No se volverá a mostrar)
                ============================================================
                """, correo, clave);
        } else {
            log.warn("Administrador inicial creado: {} (contraseña tomada de VIGENTIS_ADMIN_CLAVE)", correo);
        }
    }

    private static String generarClave() {
        SecureRandom azar = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            sb.append(CARACTERES.charAt(azar.nextInt(CARACTERES.length())));
        }
        sb.append(azar.nextInt(10));   // garantiza al menos un número
        return sb.toString();
    }
}

package com.vigentis.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API:
 * - Login con correo y contraseña -> se entrega un token JWT firmado.
 * - Cada petición debe traer el token (cabecera Authorization: Bearer ...).
 * - Cada ruta exige el permiso que corresponde al módulo.
 */
@Configuration
public class SeguridadConfig {

    private static final Logger log = LoggerFactory.getLogger(SeguridadConfig.class);

    private final SecretKey clave;

    public SeguridadConfig(@Value("${vigentis.jwt.secreto:}") String secreto) {
        byte[] bytes;
        if (secreto == null || secreto.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET no está configurado: se generó una clave temporal. "
                    + "Las sesiones se cerrarán cada vez que se reinicie el backend.");
        } else {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
            }
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, ConvertidorJwt convertidor) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                 // API sin cookies: se usa token
            .cors(Customizer.withDefaults())              // usa la configuración de WebConfig
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // Públicas
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()

                // Sesión propia y datos generales
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/dashboard").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/usuarios/opciones").authenticated()

                // Activos
                .requestMatchers(HttpMethod.GET, "/api/activos/**").hasAuthority("ACTIVOS_VER")
                .requestMatchers("/api/activos/**").hasAuthority("ACTIVOS_GESTIONAR")

                // Criterios de riesgo: todos los ven; solo quien tenga el permiso los cambia
                .requestMatchers(HttpMethod.GET, "/api/criterios-riesgo").authenticated()
                .requestMatchers("/api/criterios-riesgo").hasAuthority("CRITERIOS_GESTIONAR")

                // Riesgos (incluye bitácora y controles del riesgo)
                .requestMatchers(HttpMethod.GET, "/api/riesgos/**").hasAuthority("RIESGOS_VER")
                .requestMatchers(HttpMethod.POST, "/api/riesgos/*/aceptar").hasAuthority("RIESGOS_ACEPTAR")
                .requestMatchers("/api/riesgos/**").hasAuthority("RIESGOS_GESTIONAR")

                // Controles ISO
                .requestMatchers(HttpMethod.GET, "/api/controles/**").hasAuthority("CONTROLES_VER")
                .requestMatchers("/api/controles/**").hasAuthority("CONTROLES_GESTIONAR")

                // Incidentes
                .requestMatchers(HttpMethod.GET, "/api/incidentes/**").hasAuthority("INCIDENTES_VER")
                .requestMatchers("/api/incidentes/**").hasAuthority("INCIDENTES_GESTIONAR")

                // Auditorías y hallazgos
                .requestMatchers(HttpMethod.GET, "/api/auditorias/**", "/api/hallazgos/**").hasAuthority("AUDITORIAS_VER")
                .requestMatchers("/api/auditorias/**", "/api/hallazgos/**").hasAuthority("AUDITORIAS_IMPORTAR")

                // Documentos
                .requestMatchers(HttpMethod.GET, "/api/documentos/**").hasAuthority("DOCUMENTOS_VER")
                .requestMatchers(HttpMethod.POST, "/api/documentos/*/aprobar").hasAuthority("DOCUMENTOS_APROBAR")
                .requestMatchers("/api/documentos/**").hasAuthority("DOCUMENTOS_GESTIONAR")

                // Reportes e historial
                .requestMatchers("/api/reportes/**").hasAuthority("REPORTES_VER")
                .requestMatchers("/api/historial/**").hasAuthority("HISTORIAL_VER")

                // Usuarios, roles y permisos
                .requestMatchers(HttpMethod.GET, "/api/roles/**", "/api/permisos/**")
                    .hasAnyAuthority("USUARIOS_GESTIONAR", "ROLES_GESTIONAR")
                .requestMatchers("/api/roles/**").hasAuthority("ROLES_GESTIONAR")
                .requestMatchers("/api/usuarios/**").hasAuthority("USUARIOS_GESTIONAR")

                // Cualquier otra ruta de la API: solo con sesión
                .requestMatchers("/api/**").authenticated()

                // La web (React) y sus archivos son públicos; los datos siguen protegidos en /api
                .anyRequest().permitAll())
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(convertidor))
                .authenticationEntryPoint((req, res, ex) ->
                        responder(res, 401, "Debes iniciar sesión (o tu sesión expiró)")))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                        responder(res, 401, "Debes iniciar sesión (o tu sesión expiró)"))
                .accessDeniedHandler((req, res, ex) ->
                        responder(res, 403, "Tu rol no tiene permiso para esta acción")));
        return http.build();
    }

    private static void responder(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}

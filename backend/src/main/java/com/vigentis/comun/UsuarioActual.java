package com.vigentis.comun;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Quién está haciendo la acción: se toma del token de la sesión. */
public final class UsuarioActual {

    private UsuarioActual() {}

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                return Long.valueOf(jwt.getSubject());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

package com.vigentis.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Reglas mínimas para las contraseñas. */
public final class ReglasClave {

    public static final int LONGITUD_MINIMA = 10;

    private ReglasClave() {}

    public static void validar(String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            throw error("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        if (clave.length() > 72) {
            throw error("La contraseña no puede tener más de 72 caracteres");
        }
        boolean tieneLetra = clave.chars().anyMatch(Character::isLetter);
        boolean tieneNumero = clave.chars().anyMatch(Character::isDigit);
        if (!tieneLetra || !tieneNumero) {
            throw error("La contraseña debe combinar letras y números");
        }
    }

    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

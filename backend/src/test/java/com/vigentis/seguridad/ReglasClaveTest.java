package com.vigentis.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Reglas de contraseña (A.5.17, A.8.5)")
class ReglasClaveTest {

    @Test
    @DisplayName("Acepta una contraseña de 10+ caracteres con letras y números")
    void aceptaClaveValida() {
        assertDoesNotThrow(() -> ReglasClave.validar("Vigentis2026"));
    }

    @Test
    @DisplayName("Rechaza contraseñas cortas")
    void rechazaCorta() {
        var e = assertThrows(ResponseStatusException.class, () -> ReglasClave.validar("abc123"));
        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    @DisplayName("Rechaza contraseñas sin números o sin letras")
    void rechazaSinCombinacion() {
        assertThrows(ResponseStatusException.class, () -> ReglasClave.validar("soloLetrasAqui"));
        assertThrows(ResponseStatusException.class, () -> ReglasClave.validar("12345678901"));
    }

    @Test
    @DisplayName("Rechaza contraseñas de más de 72 caracteres (límite de BCrypt)")
    void rechazaMuyLarga() {
        assertThrows(ResponseStatusException.class, () -> ReglasClave.validar("a1".repeat(37)));
    }

    @Test
    @DisplayName("Rechaza una contraseña vacía")
    void rechazaNula() {
        assertThrows(ResponseStatusException.class, () -> ReglasClave.validar(null));
    }
}

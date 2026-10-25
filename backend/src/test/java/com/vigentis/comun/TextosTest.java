package com.vigentis.comun;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Limpieza de textos de los formularios")
class TextosTest {

    @Test
    @DisplayName("Un texto vacío o con solo espacios se guarda como nulo")
    void limpio() {
        assertNull(Textos.limpio("   "));
        assertNull(Textos.limpio(null));
        assertEquals("Servidor", Textos.limpio("  Servidor "));
    }

    @Test
    @DisplayName("Los encabezados del CSV se normalizan sin tildes ni mayúsculas")
    void clave() {
        assertEquals("fecha_limite", Textos.clave(" Fecha Límite "));
        assertEquals("accion_correctiva", Textos.clave("Acción-Correctiva"));
    }

    @Test
    @DisplayName("Recorta textos al máximo permitido")
    void recortar() {
        assertEquals("abc", Textos.recortar("abcdef", 3));
        assertEquals("ab", Textos.recortar("ab", 3));
    }
}

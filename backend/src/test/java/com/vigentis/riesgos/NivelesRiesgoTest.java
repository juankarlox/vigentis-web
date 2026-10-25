package com.vigentis.riesgos;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Clasificación del nivel de riesgo (ISO/IEC 27001 6.1.2)")
class NivelesRiesgoTest {

    @AfterEach
    void restaurar() {
        NivelesRiesgo.configurarCargador(id -> NivelesRiesgo.POR_DEFECTO);
        for (long id = 1; id <= 3; id++) NivelesRiesgo.olvidar(id);
    }

    @ParameterizedTest(name = "nivel {0} -> {1}")
    @CsvSource({"1,BAJO", "4,BAJO", "5,MEDIO", "9,MEDIO", "10,ALTO", "19,ALTO", "20,CRITICO", "25,CRITICO"})
    @DisplayName("Con los criterios por defecto, los límites de cada categoría son exactos")
    void clasificaConCriteriosPorDefecto(int valor, String esperado) {
        assertEquals(esperado, NivelesRiesgo.clasificar(1L, valor));
    }

    @Test
    @DisplayName("Un riesgo sin evaluar no tiene nivel")
    void sinValorNoHayNivel() {
        assertNull(NivelesRiesgo.clasificar(1L, null));
    }

    @Test
    @DisplayName("Cada empresa clasifica con sus propios umbrales")
    void criteriosPorEmpresa() {
        NivelesRiesgo.configurarCargador(id -> id == 2L
                ? new NivelesRiesgo.Umbrales(3, 6, 12, 2)
                : NivelesRiesgo.POR_DEFECTO);
        assertEquals("ALTO", NivelesRiesgo.clasificar(2L, 8));    // empresa 2: alto desde 6
        assertEquals("MEDIO", NivelesRiesgo.clasificar(1L, 8));   // empresa 1: alto desde 10
        assertEquals("CRITICO", NivelesRiesgo.clasificar(2L, 12));
    }

    @Test
    @DisplayName("Al cambiar los criterios, se vuelven a leer los umbrales de esa empresa")
    void olvidarRecargaCriterios() {
        assertEquals("MEDIO", NivelesRiesgo.clasificar(3L, 8));
        NivelesRiesgo.configurarCargador(id -> new NivelesRiesgo.Umbrales(2, 4, 8, 1));
        assertEquals("MEDIO", NivelesRiesgo.clasificar(3L, 8), "sigue en caché hasta olvidar");
        NivelesRiesgo.olvidar(3L);
        assertEquals("CRITICO", NivelesRiesgo.clasificar(3L, 8));
    }

    @Test
    @DisplayName("El nivel aceptable define qué riesgos se pueden aceptar sin excepción")
    void nivelAceptable() {
        assertTrue(NivelesRiesgo.esAceptable(1L, 4));
        assertFalse(NivelesRiesgo.esAceptable(1L, 5));
        assertFalse(NivelesRiesgo.esAceptable(1L, null));
        assertEquals(4, NivelesRiesgo.nivelAceptable(1L));
    }
}

package com.vigentis.comun;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Lectura y escritura de CSV (importación de hallazgos y reportes)")
class CsvTest {

    @Test
    @DisplayName("Lee un CSV con punto y coma, como lo guarda Excel en español")
    void leePuntoYComa() {
        var filas = Csv.leer("codigo;tipo;descripcion\nH-01;NC_MENOR;Sin revisión de accesos\n");
        assertEquals(2, filas.size());
        assertEquals(List.of("H-01", "NC_MENOR", "Sin revisión de accesos"), filas.get(1));
    }

    @Test
    @DisplayName("Detecta la coma como separador y quita el BOM inicial")
    void detectaComaYBom() {
        var filas = Csv.leer("﻿codigo,tipo\r\nH-02,OBSERVACION\r\n");
        assertEquals("codigo", filas.get(0).get(0));
        assertEquals("OBSERVACION", filas.get(1).get(1));
    }

    @Test
    @DisplayName("Respeta campos entre comillas con separadores, comillas y saltos de línea")
    void camposEntreComillas() {
        var filas = Csv.leer("a;b\n\"uno; dos\";\"dijo \"\"hola\"\"\notra línea\"\n");
        assertEquals("uno; dos", filas.get(1).get(0));
        assertEquals("dijo \"hola\"\notra línea", filas.get(1).get(1));
    }

    @Test
    @DisplayName("Ignora las filas vacías")
    void ignoraFilasVacias() {
        assertEquals(2, Csv.leer("a;b\n;\n\n1;2\n").size());
    }

    @Test
    @DisplayName("Lo que genera un reporte se vuelve a leer igual (ida y vuelta)")
    void idaYVuelta() {
        byte[] csv = Csv.generar(List.of("Código", "Detalle"),
                List.of(List.<Object>of("RSG-001", "Phishing; robo de \"claves\"")));
        var filas = Csv.leer(new String(csv, StandardCharsets.UTF_8));
        assertEquals(List.of("Código", "Detalle"), filas.get(0));
        assertEquals("Phishing; robo de \"claves\"", filas.get(1).get(1));
    }
}

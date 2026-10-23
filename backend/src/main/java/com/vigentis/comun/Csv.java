package com.vigentis.comun;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee y escribe CSV. Usa punto y coma (;) porque es lo que espera Excel en español.
 */
public final class Csv {

    private Csv() {}

    // ---------- Escribir ----------

    /** Genera un CSV listo para abrir en Excel (UTF-8 con BOM para que se vean las tildes). */
    public static byte[] generar(List<String> encabezados, List<List<Object>> filas) {
        StringBuilder sb = new StringBuilder("﻿");
        agregarFila(sb, new ArrayList<>(encabezados));
        for (List<Object> fila : filas) {
            agregarFila(sb, fila);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void agregarFila(StringBuilder sb, List<?> valores) {
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) sb.append(';');
            Object v = valores.get(i);
            String texto = v == null ? "" : v.toString();
            if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
                texto = "\"" + texto.replace("\"", "\"\"") + "\"";
            }
            sb.append(texto);
        }
        sb.append("\r\n");
    }

    // ---------- Leer ----------

    /** Convierte el texto de un CSV en filas y columnas. Detecta si usa ; o , como separador. */
    public static List<List<String>> leer(String contenido) {
        if (contenido.startsWith("﻿")) contenido = contenido.substring(1);
        int finPrimeraLinea = contenido.indexOf('\n');
        String primeraLinea = finPrimeraLinea < 0 ? contenido : contenido.substring(0, finPrimeraLinea);
        char separador = primeraLinea.contains(";") ? ';' : ',';

        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < contenido.length() && contenido.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == separador) {
                fila.add(campo.toString().trim());
                campo.setLength(0);
            } else if (c == '\n') {
                fila.add(campo.toString().trim());
                campo.setLength(0);
                filas.add(fila);
                fila = new ArrayList<>();
            } else if (c != '\r') {
                campo.append(c);
            }
        }
        if (campo.length() > 0 || !fila.isEmpty()) {
            fila.add(campo.toString().trim());
            filas.add(fila);
        }
        // Quitar filas totalmente vacías
        filas.removeIf(f -> f.stream().allMatch(String::isEmpty));
        return filas;
    }
}

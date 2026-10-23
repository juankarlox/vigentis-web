package com.vigentis.comun;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades pequeñas para limpiar textos. */
public final class Textos {

    private Textos() {}

    /** null si viene vacío; si no, el texto sin espacios a los lados. */
    public static String limpio(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    public static String recortar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    /** "Fecha Límite" -> "fecha_limite" (sin tildes, minúsculas, con guion bajo). */
    public static String clave(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s\\-]+", "_");
    }
}

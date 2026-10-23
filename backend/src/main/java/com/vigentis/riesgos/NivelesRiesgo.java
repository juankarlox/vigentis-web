package com.vigentis.riesgos;

import com.vigentis.comun.EmpresaActual;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Umbrales para clasificar un riesgo (BAJO / MEDIO / ALTO / CRITICO), POR EMPRESA.
 * Cada empresa los define en "Criterios de riesgo"; aquí se guardan en memoria para
 * no consultar la base de datos cada vez que se muestra un riesgo.
 */
public final class NivelesRiesgo {

    public record Umbrales(int medio, int alto, int critico, int aceptable) {}

    public static final Umbrales POR_DEFECTO = new Umbrales(5, 10, 20, 4);

    private static final Map<Long, Umbrales> CACHE = new ConcurrentHashMap<>();
    private static volatile Function<Long, Umbrales> cargador = id -> POR_DEFECTO;

    private NivelesRiesgo() {}

    /** Lo configura CriteriosService al arrancar: cómo leer los umbrales de una empresa. */
    static void configurarCargador(Function<Long, Umbrales> nuevo) {
        cargador = nuevo;
    }

    /** Se llama cuando una empresa cambia sus criterios. */
    static void olvidar(Long empresaId) {
        if (empresaId != null) CACHE.remove(empresaId);
    }

    static Umbrales de(Long empresaId) {
        if (empresaId == null) return POR_DEFECTO;
        Umbrales u = CACHE.get(empresaId);
        if (u == null) {
            u = cargador.apply(empresaId);
            CACHE.put(empresaId, u);
        }
        return u;
    }

    public static String clasificar(Long empresaId, Integer valor) {
        if (valor == null) return null;
        Umbrales u = de(empresaId);
        if (valor >= u.critico()) return "CRITICO";
        if (valor >= u.alto()) return "ALTO";
        if (valor >= u.medio()) return "MEDIO";
        return "BAJO";
    }

    /** Clasifica con los criterios de la empresa de la sesión actual. */
    public static String clasificar(Integer valor) {
        return clasificar(EmpresaActual.id(), valor);
    }

    public static boolean esAceptable(Long empresaId, Integer valor) {
        return valor != null && valor <= de(empresaId).aceptable();
    }

    public static int nivelAceptable(Long empresaId) {
        return de(empresaId).aceptable();
    }
}

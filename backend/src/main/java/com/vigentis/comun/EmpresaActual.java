package com.vigentis.comun;

import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * A qué empresa pertenece la petición actual.
 * Normalmente sale del token de sesión (dato "empresa"); en el registro o al arrancar
 * se puede fijar a mano con {@link #como(Long, Supplier)}.
 */
public final class EmpresaActual {

    private static final ThreadLocal<Long> FIJADA = new ThreadLocal<>();

    private EmpresaActual() {}

    /** Id de la empresa actual, o null si no hay sesión (ej.: en el login). */
    public static Long id() {
        Long fijada = FIJADA.get();
        if (fijada != null) return fijada;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            Object valor = jwt.getClaims().get("empresa");
            if (valor instanceof Number n) return n.longValue();
            if (valor != null) {
                try {
                    return Long.valueOf(valor.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Ejecuta algo "dentro" de una empresa concreta (para el registro y tareas internas). */
    public static <T> T como(Long empresaId, Supplier<T> accion) {
        Long anterior = FIJADA.get();
        FIJADA.set(empresaId);
        try {
            return accion.get();
        } finally {
            if (anterior == null) FIJADA.remove();
            else FIJADA.set(anterior);
        }
    }

    /** true si el registro pertenece a la empresa actual (defensa extra contra accesos cruzados). */
    public static boolean esDeLaEmpresa(DeEmpresa registro) {
        Long actual = id();
        return actual != null && actual.equals(registro.empresa());
    }
}

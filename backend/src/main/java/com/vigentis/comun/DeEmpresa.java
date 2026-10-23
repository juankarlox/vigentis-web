package com.vigentis.comun;

/** Cualquier registro que pertenece a una empresa. */
public interface DeEmpresa {

    /** Id de la empresa dueña del registro (se llama "empresa" y no "getEmpresa" para no enviarlo a la web). */
    Long empresa();
}

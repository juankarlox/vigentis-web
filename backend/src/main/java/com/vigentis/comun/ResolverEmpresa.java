package com.vigentis.comun;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Le dice a Hibernate cuál es la empresa actual. Con eso, TODAS las consultas a las
 * entidades marcadas con @TenantId se filtran solas por empresa y los registros nuevos
 * se guardan con la empresa correcta.
 *
 * Sin sesión (login, registro, arranque) se usa la empresa "0" = acceso del sistema sin filtro.
 */
public class ResolverEmpresa implements CurrentTenantIdentifierResolver<Long> {

    public static final Long SISTEMA = 0L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long id = EmpresaActual.id();
        return id == null ? SISTEMA : id;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(Long tenantId) {
        return SISTEMA.equals(tenantId);
    }
}

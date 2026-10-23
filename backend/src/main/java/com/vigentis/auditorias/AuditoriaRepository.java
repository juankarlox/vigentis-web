package com.vigentis.auditorias;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 5) AS SIGNED)), 0) FROM auditorias WHERE empresa_id = :empresa",
           nativeQuery = true)
    Long ultimoNumero(@Param("empresa") Long empresa);

    @Query("""
        SELECT a FROM Auditoria a
        WHERE (:texto IS NULL
               OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:tipo IS NULL OR a.tipo = :tipo)
        ORDER BY a.fechaAuditoria DESC, a.id DESC
        """)
    List<Auditoria> buscar(@Param("texto") String texto, @Param("tipo") Auditoria.Tipo tipo);
}

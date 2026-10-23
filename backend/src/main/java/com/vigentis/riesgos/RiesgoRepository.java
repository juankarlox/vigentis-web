package com.vigentis.riesgos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 5) AS SIGNED)), 0) FROM riesgos WHERE empresa_id = :empresa",
           nativeQuery = true)
    Long ultimoNumero(@Param("empresa") Long empresa);

    // Primero los más graves
    @Query("""
        SELECT r FROM Riesgo r LEFT JOIN r.activo a
        WHERE (:texto IS NULL
               OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(r.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:estado IS NULL OR r.estado = :estado)
          AND (:activoId IS NULL OR a.id = :activoId)
        ORDER BY r.nivelValor DESC, r.codigo
        """)
    List<Riesgo> buscar(@Param("texto") String texto,
                        @Param("estado") Riesgo.Estado estado,
                        @Param("activoId") Long activoId);
}
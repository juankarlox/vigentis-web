package com.vigentis.incidentes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 5) AS SIGNED)), 0) FROM incidentes WHERE empresa_id = :empresa",
           nativeQuery = true)
    Long ultimoNumero(@Param("empresa") Long empresa);

    // Los más recientes primero
    @Query("""
        SELECT i FROM Incidente i LEFT JOIN i.activo a
        WHERE (:texto IS NULL
               OR LOWER(i.titulo) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(i.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:estado IS NULL OR i.estado = :estado)
          AND (:severidad IS NULL OR i.severidad = :severidad)
          AND (:tipo IS NULL OR i.tipo = :tipo)
          AND (:activoId IS NULL OR a.id = :activoId)
        ORDER BY i.fechaDeteccion DESC
        """)
    List<Incidente> buscar(@Param("texto") String texto,
                           @Param("estado") Incidente.Estado estado,
                           @Param("severidad") Incidente.Severidad severidad,
                           @Param("tipo") Incidente.Tipo tipo,
                           @Param("activoId") Long activoId);
}

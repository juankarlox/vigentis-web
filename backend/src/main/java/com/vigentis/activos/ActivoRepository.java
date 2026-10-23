package com.vigentis.activos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivoRepository extends JpaRepository<Activo, Long> {

    // Número más alto usado en los códigos (ACT-007 -> 7)
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo, 5) AS SIGNED)), 0) FROM activos WHERE empresa_id = :empresa",
           nativeQuery = true)
    Long ultimoNumero(@Param("empresa") Long empresa);

    @Query("""
        SELECT a FROM Activo a
        WHERE (:texto IS NULL
               OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:tipo IS NULL OR a.tipo = :tipo)
          AND (:estado IS NULL OR a.estado = :estado)
        ORDER BY a.codigo
        """)
    List<Activo> buscar(@Param("texto") String texto,
                        @Param("tipo") Activo.Tipo tipo,
                        @Param("estado") Activo.Estado estado);
}
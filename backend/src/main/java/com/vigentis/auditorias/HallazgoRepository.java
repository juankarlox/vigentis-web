package com.vigentis.auditorias;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {

    List<Hallazgo> findByAuditoriaIdOrderByCodigoAsc(Long auditoriaId);

    boolean existsByAuditoriaIdAndCodigo(Long auditoriaId, String codigo);

    long countByAuditoriaId(Long auditoriaId);

    @Query("""
        SELECT h FROM Hallazgo h
        WHERE (:estado IS NULL OR h.estado = :estado)
          AND (:tipo IS NULL OR h.tipo = :tipo)
        ORDER BY h.fechaLimite ASC, h.id ASC
        """)
    List<Hallazgo> buscar(@Param("estado") Hallazgo.Estado estado, @Param("tipo") Hallazgo.Tipo tipo);
}

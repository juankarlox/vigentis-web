package com.vigentis.historial;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistorialRepository extends JpaRepository<Historial, Long> {

    List<Historial> findTop10ByOrderByIdDesc();

    @Query("""
        SELECT h FROM Historial h
        WHERE (:entidad IS NULL OR h.entidad = :entidad)
          AND (:accion IS NULL OR h.accion = :accion)
          AND (:texto IS NULL
               OR LOWER(h.detalle) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(h.entidadCodigo) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:desde IS NULL OR h.fecha >= :desde)
          AND (:hasta IS NULL OR h.fecha < :hasta)
        ORDER BY h.id DESC
        """)
    List<Historial> buscar(@Param("entidad") String entidad,
                           @Param("accion") Historial.Accion accion,
                           @Param("texto") String texto,
                           @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta,
                           Pageable pagina);
}

package com.vigentis.controles;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ControlRepository extends JpaRepository<Control, Long> {

    Optional<Control> findByCodigoIso(String codigoIso);

    @Query("""
        SELECT c FROM Control c
        WHERE (:texto IS NULL
               OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.codigoIso) LIKE LOWER(CONCAT('%', :texto, '%')))
          AND (:tema IS NULL OR c.tema = :tema)
          AND (:estado IS NULL OR c.estado = :estado)
          AND (:aplica IS NULL OR c.aplica = :aplica)
        ORDER BY c.id
        """)
    List<Control> buscar(@Param("texto") String texto,
                         @Param("tema") Control.Tema tema,
                         @Param("estado") Control.Estado estado,
                         @Param("aplica") Boolean aplica);

    // ----- Relación riesgo <-> control (tabla riesgo_control) -----

    @Query(value = """
        SELECT c.* FROM controles c
        JOIN riesgo_control rc ON rc.control_id = c.id
        WHERE rc.riesgo_id = :riesgoId
        ORDER BY c.id
        """, nativeQuery = true)
    List<Control> buscarPorRiesgo(@Param("riesgoId") Long riesgoId);

    @Query(value = "SELECT COUNT(*) FROM riesgo_control WHERE riesgo_id = :r AND control_id = :c",
           nativeQuery = true)
    long contarVinculo(@Param("r") Long riesgoId, @Param("c") Long controlId);

    @Modifying
    @Query(value = "INSERT INTO riesgo_control (riesgo_id, control_id) VALUES (:r, :c)",
           nativeQuery = true)
    void vincular(@Param("r") Long riesgoId, @Param("c") Long controlId);

    @Modifying
    @Query(value = "DELETE FROM riesgo_control WHERE riesgo_id = :r AND control_id = :c",
           nativeQuery = true)
    int desvincular(@Param("r") Long riesgoId, @Param("c") Long controlId);
}
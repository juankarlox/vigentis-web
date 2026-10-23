package com.vigentis.riesgos;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriteriosRiesgoRepository extends JpaRepository<CriteriosRiesgo, Long> {

    // Se filtra solo por la empresa actual (multi-empresa): cada empresa tiene una fila
    Optional<CriteriosRiesgo> findFirstByOrderByIdAsc();
}

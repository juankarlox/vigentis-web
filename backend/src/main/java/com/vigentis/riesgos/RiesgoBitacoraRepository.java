package com.vigentis.riesgos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiesgoBitacoraRepository extends JpaRepository<RiesgoBitacora, Long> {

    List<RiesgoBitacora> findByRiesgoIdOrderByFechaDescIdDesc(Long riesgoId);
}
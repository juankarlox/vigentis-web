package com.vigentis.usuarios;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findAllByOrderByModuloAscCodigoAsc();

    List<Permiso> findByCodigoIn(Collection<String> codigos);
}

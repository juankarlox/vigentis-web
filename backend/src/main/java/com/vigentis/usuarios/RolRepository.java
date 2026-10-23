package com.vigentis.usuarios;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

    List<Rol> findAllByOrderByNombreAsc();

    Optional<Rol> findByNombreIgnoreCase(String nombre);

    Optional<Rol> findFirstByEsSistemaTrue();
}

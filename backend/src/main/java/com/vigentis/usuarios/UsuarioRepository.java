package com.vigentis.usuarios;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    List<Usuario> findAllByOrderByNombreAsc();

    List<Usuario> findByActivoTrueOrderByNombreAsc();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.activo = true AND u.rol.esSistema = true")
    long contarAdministradoresActivos();
}

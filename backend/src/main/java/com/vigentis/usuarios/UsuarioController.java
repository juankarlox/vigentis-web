package com.vigentis.usuarios;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UsuarioController {

    private final UsuarioService usuarios;
    private final RolService roles;

    public UsuarioController(UsuarioService usuarios, RolService roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    // ----- Usuarios -----

    @GetMapping("/api/usuarios")
    public List<Usuario> listar() {
        return usuarios.listar();
    }

    // Lista corta (id y nombre) para elegir responsables; la puede ver cualquier usuario con sesión
    @GetMapping("/api/usuarios/opciones")
    public List<UsuarioService.Opcion> opciones() {
        return usuarios.opciones();
    }

    @GetMapping("/api/usuarios/{id}")
    public Usuario obtener(@PathVariable Long id) {
        return usuarios.obtener(id);
    }

    @PostMapping("/api/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario crear(@Valid @RequestBody UsuarioRequest datos) {
        return usuarios.crear(datos);
    }

    @PutMapping("/api/usuarios/{id}")
    public Usuario actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest datos) {
        return usuarios.actualizar(id, datos);
    }

    @PostMapping("/api/usuarios/{id}/desbloquear")
    public Usuario desbloquear(@PathVariable Long id) {
        return usuarios.desbloquear(id);
    }

    // ----- Roles y permisos -----

    @GetMapping("/api/roles")
    public List<Rol> roles() {
        return roles.listar();
    }

    @GetMapping("/api/permisos")
    public List<Permiso> permisos() {
        return roles.permisos();
    }

    @PostMapping("/api/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public Rol crearRol(@Valid @RequestBody RolRequest datos) {
        return roles.crear(datos);
    }

    @PutMapping("/api/roles/{id}")
    public Rol actualizarRol(@PathVariable Long id, @Valid @RequestBody RolRequest datos) {
        return roles.actualizar(id, datos);
    }

    @DeleteMapping("/api/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarRol(@PathVariable Long id) {
        roles.eliminar(id);
    }
}

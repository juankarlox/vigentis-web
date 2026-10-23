package com.vigentis.usuarios;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RolService {

    private final RolRepository repo;
    private final PermisoRepository permisos;
    private final HistorialService historial;

    public RolService(RolRepository repo, PermisoRepository permisos, HistorialService historial) {
        this.repo = repo;
        this.permisos = permisos;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public List<Rol> listar() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Permiso> permisos() {
        return permisos.findAllByOrderByModuloAscCodigoAsc();
    }

    @Transactional(readOnly = true)
    public Rol obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el rol con id " + id));
    }

    @Transactional
    public Rol crear(RolRequest d) {
        validarNombre(d.nombre(), null);
        Rol r = new Rol();
        copiar(d, r);
        repo.saveAndFlush(r);
        historial.registrar("ROL", r.getId(), null, Historial.Accion.CREAR,
                "Rol creado: " + r.getNombre() + " con " + r.getPermisos().size() + " permisos");
        return r;
    }

    @Transactional
    public Rol actualizar(Long id, RolRequest d) {
        Rol r = obtener(id);
        if (Boolean.TRUE.equals(r.getEsSistema())) {
            throw error("El rol de administrador del sistema no se puede modificar");
        }
        validarNombre(d.nombre(), id);
        copiar(d, r);
        repo.saveAndFlush(r);
        historial.registrar("ROL", r.getId(), null, Historial.Accion.EDITAR,
                "Rol actualizado: " + r.getNombre() + " con " + r.getPermisos().size() + " permisos");
        return r;
    }

    @Transactional
    public void eliminar(Long id) {
        Rol r = obtener(id);
        if (Boolean.TRUE.equals(r.getEsSistema())) {
            throw error("El rol de administrador del sistema no se puede eliminar");
        }
        if (r.getTotalUsuarios() != null && r.getTotalUsuarios() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el rol tiene " + r.getTotalUsuarios() + " usuario(s) asignado(s)");
        }
        repo.delete(r);
        historial.registrar("ROL", r.getId(), null, Historial.Accion.ELIMINAR, "Rol eliminado: " + r.getNombre());
    }

    private void copiar(RolRequest d, Rol r) {
        Set<String> codigos = new TreeSet<>(d.permisos());
        List<Permiso> encontrados = permisos.findByCodigoIn(codigos);
        if (encontrados.size() != codigos.size()) {
            Set<String> validos = new HashSet<>();
            encontrados.forEach(p -> validos.add(p.getCodigo()));
            codigos.removeAll(validos);
            throw error("Permisos no válidos: " + String.join(", ", codigos));
        }
        r.setNombre(d.nombre().trim());
        r.setDescripcion(d.descripcion() == null || d.descripcion().isBlank() ? null : d.descripcion().trim());
        r.setPermisos(new HashSet<>(encontrados));
    }

    private void validarNombre(String nombre, Long idActual) {
        repo.findByNombreIgnoreCase(nombre.trim()).ifPresent(otro -> {
            if (idActual == null || !otro.getId().equals(idActual)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con ese nombre");
            }
        });
    }

    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

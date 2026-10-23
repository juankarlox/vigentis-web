package com.vigentis.activos;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activos")
public class ActivoController {

    private final ActivoService servicio;

    public ActivoController(ActivoService servicio) {
        this.servicio = servicio;
    }

    // GET /api/activos?q=correo&tipo=SERVIDOR&estado=ACTIVO  (todos opcionales)
    @GetMapping
    public List<Activo> listar(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Activo.Tipo tipo,
                               @RequestParam(required = false) Activo.Estado estado) {
        return servicio.buscar(q, tipo, estado);
    }

    @GetMapping("/{id}")
    public Activo obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Activo crear(@Valid @RequestBody ActivoRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    public Activo actualizar(@PathVariable Long id, @Valid @RequestBody ActivoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}

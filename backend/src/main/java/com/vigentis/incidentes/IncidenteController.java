package com.vigentis.incidentes;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidentes")
public class IncidenteController {

    private final IncidenteService servicio;

    public IncidenteController(IncidenteService servicio) {
        this.servicio = servicio;
    }

    // GET /api/incidentes?q=correo&estado=ABIERTO&severidad=ALTA&tipo=PHISHING&activoId=1  (todos opcionales)
    @GetMapping
    public List<Incidente> listar(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Incidente.Estado estado,
                                  @RequestParam(required = false) Incidente.Severidad severidad,
                                  @RequestParam(required = false) Incidente.Tipo tipo,
                                  @RequestParam(required = false) Long activoId) {
        return servicio.buscar(q, estado, severidad, tipo, activoId);
    }

    @GetMapping("/{id}")
    public Incidente obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incidente crear(@Valid @RequestBody IncidenteRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    public Incidente actualizar(@PathVariable Long id, @Valid @RequestBody IncidenteRequest datos) {
        return servicio.actualizar(id, datos);
    }
}

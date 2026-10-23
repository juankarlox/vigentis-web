package com.vigentis.controles;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class ControlController {

    private final ControlService servicio;

    public ControlController(ControlService servicio) {
        this.servicio = servicio;
    }

    // GET /api/controles?q=acceso&tema=TECNOLOGICO&estado=IMPLEMENTADO&aplica=true  (todos opcionales)
    @GetMapping("/api/controles")
    public List<Control> listar(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Control.Tema tema,
                                @RequestParam(required = false) Control.Estado estado,
                                @RequestParam(required = false) Boolean aplica) {
        return servicio.buscar(q, tema, estado, aplica);
    }

    @GetMapping("/api/controles/resumen")
    public ControlService.Resumen resumen() {
        return servicio.resumen();
    }

    @GetMapping("/api/controles/{id}")
    public Control obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PutMapping("/api/controles/{id}")
    public Control actualizar(@PathVariable Long id, @Valid @RequestBody ControlRequest datos) {
        return servicio.actualizar(id, datos);
    }

    // ----- Controles de un riesgo -----

    @GetMapping("/api/riesgos/{riesgoId}/controles")
    public List<Control> controlesDeRiesgo(@PathVariable Long riesgoId) {
        return servicio.controlesDeRiesgo(riesgoId);
    }

    @PostMapping("/api/riesgos/{riesgoId}/controles/{controlId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Control vincular(@PathVariable Long riesgoId, @PathVariable Long controlId) {
        return servicio.vincular(riesgoId, controlId);
    }

    @DeleteMapping("/api/riesgos/{riesgoId}/controles/{controlId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular(@PathVariable Long riesgoId, @PathVariable Long controlId) {
        servicio.desvincular(riesgoId, controlId);
    }
}
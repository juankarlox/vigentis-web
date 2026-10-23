package com.vigentis.riesgos;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/criterios-riesgo")
public class CriteriosController {

    private final CriteriosService servicio;

    public CriteriosController(CriteriosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public CriteriosRiesgo obtener() {
        return servicio.obtener();
    }

    @PutMapping
    public CriteriosRiesgo actualizar(@Valid @RequestBody CriteriosRequest datos) {
        return servicio.actualizar(datos);
    }
}

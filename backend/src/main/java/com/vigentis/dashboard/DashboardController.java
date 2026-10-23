package com.vigentis.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService servicio;

    public DashboardController(DashboardService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/dashboard")
    public DashboardService.Resumen resumen() {
        return servicio.resumen();
    }
}

package com.vigentis.historial;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {

    private final HistorialService servicio;

    public HistorialController(HistorialService servicio) {
        this.servicio = servicio;
    }

    // GET /api/historial?entidad=RIESGO&accion=EDITAR&q=RSG-001&desde=2026-10-01&hasta=2026-10-31&limite=100
    @GetMapping
    public List<Historial> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Historial.Accion accion,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer limite) {
        return servicio.buscar(entidad, accion, q, desde, hasta, limite);
    }
}

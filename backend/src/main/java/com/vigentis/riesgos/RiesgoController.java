package com.vigentis.riesgos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/riesgos")
public class RiesgoController {

    public record ComentarioRequest(
            @NotBlank(message = "El comentario no puede estar vacío")
            @Size(max = 2000, message = "Máximo 2000 caracteres")
            String comentario) {}

    public record TextoRequest(
            @Size(max = 2000, message = "Máximo 2000 caracteres")
            String texto) {}

    private final RiesgoService servicio;

    public RiesgoController(RiesgoService servicio) {
        this.servicio = servicio;
    }

    // GET /api/riesgos?q=correo&estado=ABIERTO&activoId=1  (todos opcionales)
    @GetMapping
    public List<Riesgo> listar(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Riesgo.Estado estado,
                               @RequestParam(required = false) Long activoId) {
        return servicio.buscar(q, estado, activoId);
    }

    @GetMapping("/{id}")
    public Riesgo obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Riesgo crear(@Valid @RequestBody RiesgoRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/{id}")
    public Riesgo actualizar(@PathVariable Long id, @Valid @RequestBody RiesgoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @GetMapping("/{id}/bitacora")
    public List<RiesgoBitacora> bitacora(@PathVariable Long id) {
        return servicio.verBitacora(id);
    }

    // Aceptación formal (requiere el permiso RIESGOS_ACEPTAR)
    @PostMapping("/{id}/aceptar")
    public Riesgo aceptar(@PathVariable Long id, @Valid @RequestBody(required = false) TextoRequest datos) {
        return servicio.aceptar(id, datos == null ? null : datos.texto());
    }

    // Revisión periódica
    @PostMapping("/{id}/revisar")
    public Riesgo revisar(@PathVariable Long id, @Valid @RequestBody(required = false) TextoRequest datos) {
        return servicio.revisar(id, datos == null ? null : datos.texto());
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public RiesgoBitacora comentar(@PathVariable Long id, @Valid @RequestBody ComentarioRequest datos) {
        return servicio.comentar(id, datos.comentario());
    }
}
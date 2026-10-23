package com.vigentis.auditorias;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AuditoriaController {

    private final AuditoriaService servicio;

    public AuditoriaController(AuditoriaService servicio) {
        this.servicio = servicio;
    }

    // ----- Auditorías -----

    // GET /api/auditorias?q=interna&tipo=INTERNA
    @GetMapping("/api/auditorias")
    public List<Auditoria> listar(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Auditoria.Tipo tipo) {
        return servicio.buscar(q, tipo);
    }

    @GetMapping("/api/auditorias/{id}")
    public Auditoria obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping("/api/auditorias")
    @ResponseStatus(HttpStatus.CREATED)
    public Auditoria crear(@Valid @RequestBody AuditoriaRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/api/auditorias/{id}")
    public Auditoria actualizar(@PathVariable Long id, @Valid @RequestBody AuditoriaRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @DeleteMapping("/api/auditorias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    // ----- Importar resultados de una auditoría desde un CSV -----

    @PostMapping(value = "/api/auditorias/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuditoriaService.ResultadoImportacion importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("nombre") String nombre,
            @RequestParam("tipo") Auditoria.Tipo tipo,
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(value = "auditor", required = false) String auditor) {
        return servicio.importar(archivo, nombre, tipo, fecha, auditor);
    }

    @GetMapping("/api/auditorias/plantilla")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("plantilla_hallazgos.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(servicio.plantilla());
    }

    // ----- Hallazgos -----

    @GetMapping("/api/auditorias/{id}/hallazgos")
    public List<Hallazgo> hallazgosDe(@PathVariable Long id) {
        return servicio.hallazgosDe(id);
    }

    @PostMapping("/api/auditorias/{id}/hallazgos")
    @ResponseStatus(HttpStatus.CREATED)
    public Hallazgo agregarHallazgo(@PathVariable Long id, @Valid @RequestBody HallazgoRequest datos) {
        return servicio.agregarHallazgo(id, datos);
    }

    // GET /api/hallazgos?estado=ABIERTO&tipo=NC_MAYOR  (todos los hallazgos, de todas las auditorías)
    @GetMapping("/api/hallazgos")
    public List<Hallazgo> buscarHallazgos(@RequestParam(required = false) Hallazgo.Estado estado,
                                          @RequestParam(required = false) Hallazgo.Tipo tipo) {
        return servicio.buscarHallazgos(estado, tipo);
    }

    @PutMapping("/api/hallazgos/{id}")
    public Hallazgo actualizarHallazgo(@PathVariable Long id, @Valid @RequestBody HallazgoRequest datos) {
        return servicio.actualizarHallazgo(id, datos);
    }
}

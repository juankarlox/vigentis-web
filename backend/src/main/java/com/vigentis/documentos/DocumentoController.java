package com.vigentis.documentos;

import com.vigentis.controles.Control;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DocumentoController {

    private final DocumentoService servicio;

    public DocumentoController(DocumentoService servicio) {
        this.servicio = servicio;
    }

    // GET /api/documentos?q=politica&tipo=POLITICA&estado=APROBADO
    @GetMapping("/api/documentos")
    public List<Documento> listar(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) Documento.Tipo tipo,
                                  @RequestParam(required = false) Documento.Estado estado) {
        return servicio.buscar(q, tipo, estado);
    }

    @GetMapping("/api/documentos/{id}")
    public Documento obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping("/api/documentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Documento crear(@Valid @RequestBody DocumentoRequest datos) {
        return servicio.crear(datos);
    }

    @PutMapping("/api/documentos/{id}")
    public Documento actualizar(@PathVariable Long id, @Valid @RequestBody DocumentoRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @PostMapping("/api/documentos/{id}/aprobar")
    public Documento aprobar(@PathVariable Long id) {
        return servicio.aprobar(id);
    }

    // ----- Archivo -----

    @PostMapping(value = "/api/documentos/{id}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Documento subirArchivo(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return servicio.subirArchivo(id, archivo);
    }

    @GetMapping("/api/documentos/{id}/archivo")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        DocumentoService.Descarga d = servicio.descargar(id);
        MediaType tipo;
        try {
            tipo = d.tipo() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(d.tipo());
        } catch (InvalidMediaTypeException e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.nombre(), StandardCharsets.UTF_8).build().toString())
                .contentType(tipo)
                .body(d.recurso());
    }

    // ----- Controles que evidencia el documento -----

    @GetMapping("/api/documentos/{id}/controles")
    public List<Control> controlesDe(@PathVariable Long id) {
        return servicio.controlesDe(id);
    }

    @PostMapping("/api/documentos/{id}/controles/{controlId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Control vincularControl(@PathVariable Long id, @PathVariable Long controlId) {
        return servicio.vincularControl(id, controlId);
    }

    @DeleteMapping("/api/documentos/{id}/controles/{controlId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincularControl(@PathVariable Long id, @PathVariable Long controlId) {
        servicio.desvincularControl(id, controlId);
    }

    @GetMapping("/api/controles/{controlId}/documentos")
    public List<Documento> documentosDeControl(@PathVariable Long controlId) {
        return servicio.documentosDeControl(controlId);
    }
}

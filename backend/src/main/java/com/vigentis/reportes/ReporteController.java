package com.vigentis.reportes;

import com.vigentis.activos.ActivoRepository;
import com.vigentis.auditorias.HallazgoRepository;
import com.vigentis.comun.Csv;
import com.vigentis.controles.ControlRepository;
import com.vigentis.documentos.DocumentoRepository;
import com.vigentis.historial.HistorialService;
import com.vigentis.incidentes.IncidenteRepository;
import com.vigentis.riesgos.RiesgoRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reportes exportables en CSV (se abren directo en Excel). */
@RestController
@RequestMapping("/api/reportes")
@Transactional(readOnly = true)
public class ReporteController {

    private final ActivoRepository activos;
    private final RiesgoRepository riesgos;
    private final ControlRepository controles;
    private final IncidenteRepository incidentes;
    private final HallazgoRepository hallazgos;
    private final DocumentoRepository documentos;
    private final HistorialService historial;

    public ReporteController(ActivoRepository activos, RiesgoRepository riesgos, ControlRepository controles,
                             IncidenteRepository incidentes, HallazgoRepository hallazgos,
                             DocumentoRepository documentos, HistorialService historial) {
        this.activos = activos;
        this.riesgos = riesgos;
        this.controles = controles;
        this.incidentes = incidentes;
        this.hallazgos = hallazgos;
        this.documentos = documentos;
        this.historial = historial;
    }

    @GetMapping("/activos")
    public ResponseEntity<byte[]> activos() {
        List<List<Object>> filas = new ArrayList<>();
        activos.buscar(null, null, null).forEach(a -> filas.add(List.of(
                a.getCodigo(), a.getNombre(), a.getTipo(),
                a.getConfidencialidad(), a.getIntegridad(), a.getDisponibilidad(),
                v(a.getCriticidad()), a.getEstado(), v(a.getDescripcion()))));
        return csv("activos", List.of("Código", "Nombre", "Tipo", "Confidencialidad", "Integridad",
                "Disponibilidad", "Criticidad", "Estado", "Descripción"), filas);
    }

    @GetMapping("/riesgos")
    public ResponseEntity<byte[]> riesgos() {
        List<List<Object>> filas = new ArrayList<>();
        riesgos.buscar(null, null, null).forEach(r -> filas.add(List.of(
                r.getCodigo(), r.getNombre(),
                r.getActivo() == null ? "" : r.getActivo().getCodigo() + " " + r.getActivo().getNombre(),
                v(r.getAmenaza()), v(r.getVulnerabilidad()),
                r.getProbabilidad(), r.getImpacto(), v(r.getNivelValor()), v(r.getNivel()),
                v(r.getOpcionTratamiento()), v(r.getTratamiento()),
                v(r.getNivelResidual()), v(r.getNivelResidualTexto()),
                r.isDentroDeNivelAceptable() ? "Sí" : "No", r.getEstado(),
                v(r.getAceptadoPorNombre()), v(r.getAceptadoEn()), v(r.getFechaRevision()))));
        return csv("riesgos", List.of("Código", "Riesgo", "Activo", "Amenaza", "Vulnerabilidad",
                "Probabilidad", "Impacto", "Nivel", "Clasificación", "Opción de tratamiento", "Tratamiento",
                "Nivel residual", "Clasificación residual", "Dentro del nivel aceptable", "Estado",
                "Aceptado por", "Aceptado el", "Próxima revisión"), filas);
    }

    /** Declaración de Aplicabilidad (SoA). */
    @GetMapping("/controles")
    public ResponseEntity<byte[]> controles() {
        List<List<Object>> filas = new ArrayList<>();
        controles.buscar(null, null, null, null).forEach(c -> filas.add(List.of(
                c.getCodigoIso(), c.getNombre(), c.getTema(),
                c.getAplica() ? "Sí" : "No", v(c.getJustificacion()),
                c.getAplica() ? c.getEstado() : "")));
        return csv("declaracion_aplicabilidad", List.of("Control", "Nombre", "Tema", "Aplica",
                "Justificación", "Estado de implementación"), filas);
    }

    @GetMapping("/incidentes")
    public ResponseEntity<byte[]> incidentes() {
        List<List<Object>> filas = new ArrayList<>();
        incidentes.buscar(null, null, null, null, null).forEach(i -> filas.add(List.of(
                i.getCodigo(), i.getTitulo(), i.getTipo(), i.getSeveridad(), i.getEstado(),
                i.getActivo() == null ? "" : i.getActivo().getCodigo() + " " + i.getActivo().getNombre(),
                i.getFechaDeteccion(), v(i.getFechaCierre()), v(i.getAcciones()))));
        return csv("incidentes", List.of("Código", "Título", "Tipo", "Severidad", "Estado", "Activo",
                "Detección", "Cierre", "Acciones tomadas"), filas);
    }

    @GetMapping("/hallazgos")
    public ResponseEntity<byte[]> hallazgos() {
        List<List<Object>> filas = new ArrayList<>();
        hallazgos.buscar(null, null).forEach(h -> filas.add(List.of(
                h.getCodigo(), h.getTipo(), h.getDescripcion(),
                h.getControl() == null ? "" : h.getControl().getCodigoIso(),
                h.getEstado(), v(h.getFechaLimite()), h.isVencido() ? "Sí" : "No", v(h.getAccionCorrectiva()))));
        return csv("hallazgos", List.of("Código", "Tipo", "Descripción", "Control", "Estado",
                "Fecha límite", "Vencido", "Acción correctiva"), filas);
    }

    @GetMapping("/documentos")
    public ResponseEntity<byte[]> documentos() {
        List<List<Object>> filas = new ArrayList<>();
        documentos.buscar(null, null, null).forEach(d -> filas.add(List.of(
                d.getCodigo(), d.getTitulo(), d.getTipo(), d.getVersion(), d.getEstado(),
                v(d.getFechaAprobacion()), v(d.getFechaRevision()), v(d.getArchivoNombre()))));
        return csv("documentos", List.of("Código", "Título", "Tipo", "Versión", "Estado",
                "Aprobado el", "Próxima revisión", "Archivo"), filas);
    }

    @GetMapping("/historial")
    public ResponseEntity<byte[]> historial() {
        List<List<Object>> filas = new ArrayList<>();
        historial.buscar(null, null, null, null, null, 1000).forEach(h -> filas.add(List.of(
                h.getFecha(), v(h.getUsuarioNombre()), h.getEntidad(), v(h.getEntidadCodigo()),
                h.getAccion(), v(h.getDetalle()))));
        return csv("historial", List.of("Fecha", "Usuario", "Módulo", "Código", "Acción", "Detalle"), filas);
    }

    // List.of no acepta null: los vacíos se envían como ""
    private static Object v(Object valor) {
        return valor == null ? "" : valor;
    }

    private static ResponseEntity<byte[]> csv(String nombre, List<String> encabezados, List<List<Object>> filas) {
        String archivo = "vigentis_" + nombre + "_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo).build().toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(Csv.generar(encabezados, filas));
    }
}

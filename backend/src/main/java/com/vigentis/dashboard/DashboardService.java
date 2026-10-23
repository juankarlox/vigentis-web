package com.vigentis.dashboard;

import com.vigentis.activos.Activo;
import com.vigentis.activos.ActivoRepository;
import com.vigentis.auditorias.Hallazgo;
import com.vigentis.auditorias.HallazgoRepository;
import com.vigentis.controles.ControlService;
import com.vigentis.documentos.Documento;
import com.vigentis.documentos.DocumentoRepository;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import com.vigentis.incidentes.Incidente;
import com.vigentis.incidentes.IncidenteRepository;
import com.vigentis.riesgos.CriteriosRiesgo;
import com.vigentis.riesgos.CriteriosService;
import com.vigentis.riesgos.Riesgo;
import com.vigentis.riesgos.RiesgoRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    /** Todo lo que muestra la pantalla de inicio, en una sola respuesta. */
    public record Resumen(
            long activos,
            Map<String, Long> activosPorCriticidad,
            long riesgosVigentes,
            long riesgosAbiertos,
            Map<String, Long> riesgosPorNivel,
            int[][] matrizRiesgos,
            List<Riesgo> riesgosPrioritarios,
            long riesgosSobreNivelAceptable,
            long riesgosRevisionVencida,
            CriteriosRiesgo criterios,
            long incidentesAbiertos,
            Map<String, Long> incidentesAbiertosPorSeveridad,
            long incidentesUltimos30Dias,
            ControlService.Resumen controles,
            long hallazgosAbiertos,
            long hallazgosVencidos,
            Map<String, Long> documentosPorEstado,
            long documentosRevisionPendiente,
            List<Historial> actividadReciente) {}

    private final ActivoRepository activos;
    private final RiesgoRepository riesgos;
    private final IncidenteRepository incidentes;
    private final ControlService controles;
    private final HallazgoRepository hallazgos;
    private final DocumentoRepository documentos;
    private final HistorialService historial;
    private final CriteriosService criterios;

    public DashboardService(ActivoRepository activos, RiesgoRepository riesgos, IncidenteRepository incidentes,
                            ControlService controles, HallazgoRepository hallazgos,
                            DocumentoRepository documentos, HistorialService historial,
                            CriteriosService criterios) {
        this.criterios = criterios;
        this.activos = activos;
        this.riesgos = riesgos;
        this.incidentes = incidentes;
        this.controles = controles;
        this.hallazgos = hallazgos;
        this.documentos = documentos;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        // ----- Activos (sin contar los dados de baja) -----
        List<Activo> listaActivos = activos.findAll().stream()
                .filter(a -> a.getEstado() != Activo.Estado.DADO_DE_BAJA).toList();
        Map<String, Long> porCriticidad = contador("ALTA", "MEDIA", "BAJA");
        listaActivos.forEach(a -> sumar(porCriticidad, a.getCriticidad()));

        // ----- Riesgos (vigentes = todos menos los cerrados) -----
        List<Riesgo> vigentes = riesgos.findAll().stream()
                .filter(r -> r.getEstado() != Riesgo.Estado.CERRADO).toList();
        long abiertos = vigentes.stream()
                .filter(r -> r.getEstado() == Riesgo.Estado.ABIERTO || r.getEstado() == Riesgo.Estado.EN_TRATAMIENTO)
                .count();
        Map<String, Long> porNivel = contador("CRITICO", "ALTO", "MEDIO", "BAJO");
        int[][] matriz = new int[5][5];   // [probabilidad-1][impacto-1]
        for (Riesgo r : vigentes) {
            sumar(porNivel, r.getNivel());
            matriz[r.getProbabilidad() - 1][r.getImpacto() - 1]++;
        }
        List<Riesgo> prioritarios = vigentes.stream()
                .filter(r -> r.getEstado() == Riesgo.Estado.ABIERTO || r.getEstado() == Riesgo.Estado.EN_TRATAMIENTO)
                .filter(r -> "ALTO".equals(r.getNivel()) || "CRITICO".equals(r.getNivel()))
                .sorted(Comparator.comparing(Riesgo::getNivelValor).reversed())
                .limit(5)
                .toList();

        // Riesgos que superan el nivel aceptable y no han sido aceptados formalmente
        long sobreNivel = vigentes.stream()
                .filter(r -> r.getEstado() != Riesgo.Estado.ACEPTADO && !r.isDentroDeNivelAceptable())
                .count();
        long revisionVencida = vigentes.stream().filter(Riesgo::isRevisionVencida).count();

        // ----- Incidentes -----
        List<Incidente> todosIncidentes = incidentes.findAll();
        Map<String, Long> porSeveridad = contador("CRITICA", "ALTA", "MEDIA", "BAJA");
        long incidentesAbiertos = 0;
        for (Incidente i : todosIncidentes) {
            if (i.getEstado() != Incidente.Estado.CERRADO) {
                incidentesAbiertos++;
                sumar(porSeveridad, i.getSeveridad().name());
            }
        }
        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        long ultimos30 = todosIncidentes.stream()
                .filter(i -> i.getFechaDeteccion() != null && i.getFechaDeteccion().isAfter(hace30Dias))
                .count();

        // ----- Hallazgos de auditoría -----
        List<Hallazgo> todosHallazgos = hallazgos.findAll();
        long hallazgosAbiertos = todosHallazgos.stream().filter(h -> h.getEstado() != Hallazgo.Estado.CERRADO).count();
        long hallazgosVencidos = todosHallazgos.stream().filter(Hallazgo::isVencido).count();

        // ----- Documentos -----
        List<Documento> todosDocumentos = documentos.findAll();
        Map<String, Long> porEstado = contador("APROBADO", "BORRADOR", "OBSOLETO");
        todosDocumentos.forEach(d -> sumar(porEstado, d.getEstado().name()));
        long revisionPendiente = todosDocumentos.stream().filter(Documento::isRevisionPendiente).count();

        return new Resumen(
                listaActivos.size(), porCriticidad,
                vigentes.size(), abiertos, porNivel, matriz, prioritarios,
                sobreNivel, revisionVencida, criterios.obtener(),
                incidentesAbiertos, porSeveridad, ultimos30,
                controles.resumen(),
                hallazgosAbiertos, hallazgosVencidos,
                porEstado, revisionPendiente,
                historial.recientes());
    }

    /** Mapa con las categorías en orden fijo y todas en cero. */
    private static Map<String, Long> contador(String... claves) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (String c : claves) mapa.put(c, 0L);
        return mapa;
    }

    private static void sumar(Map<String, Long> mapa, String clave) {
        if (clave != null) mapa.merge(clave, 1L, Long::sum);
    }
}

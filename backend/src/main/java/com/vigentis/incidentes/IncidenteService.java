package com.vigentis.incidentes;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.activos.Activo;
import com.vigentis.activos.ActivoRepository;
import com.vigentis.comun.Textos;
import com.vigentis.comun.UsuarioActual;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IncidenteService {

    private final IncidenteRepository repo;
    private final ActivoRepository activos;
    private final HistorialService historial;

    public IncidenteService(IncidenteRepository repo, ActivoRepository activos, HistorialService historial) {
        this.repo = repo;
        this.activos = activos;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public List<Incidente> buscar(String texto, Incidente.Estado estado, Incidente.Severidad severidad,
                                  Incidente.Tipo tipo, Long activoId) {
        return repo.buscar(Textos.limpio(texto), estado, severidad, tipo, activoId);
    }

    @Transactional(readOnly = true)
    public Incidente obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el incidente con id " + id));
    }

    @Transactional
    public Incidente crear(IncidenteRequest d) {
        Incidente i = new Incidente();
        i.setCodigo(String.format("INC-%03d", repo.ultimoNumero(EmpresaActual.id()) + 1));
        i.setReportadoPor(UsuarioActual.id());
        copiarDatos(d, i);
        repo.saveAndFlush(i);
        historial.registrar("INCIDENTE", i.getId(), i.getCodigo(), Historial.Accion.CREAR,
                "Incidente registrado: " + i.getTitulo() + " (" + i.getTipo() + ", severidad " + i.getSeveridad() + ")");
        return i;
    }

    @Transactional
    public Incidente actualizar(Long id, IncidenteRequest d) {
        Incidente i = obtener(id);
        Incidente.Estado estadoAntes = i.getEstado();
        copiarDatos(d, i);
        repo.saveAndFlush(i);
        String detalle = estadoAntes != i.getEstado()
                ? "Incidente " + i.getTitulo() + ": " + estadoAntes + " -> " + i.getEstado()
                : "Incidente actualizado: " + i.getTitulo();
        historial.registrar("INCIDENTE", i.getId(), i.getCodigo(), Historial.Accion.EDITAR, detalle);
        return i;
    }

    private void copiarDatos(IncidenteRequest d, Incidente i) {
        Activo activo = null;
        if (d.activoId() != null) {
            activo = activos.findById(d.activoId()).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el activo con id " + d.activoId()));
        }
        Incidente.Estado estado = d.estado() != null ? d.estado() : i.getEstado();
        String acciones = Textos.limpio(d.acciones());
        LocalDateTime deteccion = d.fechaDeteccion().truncatedTo(ChronoUnit.SECONDS);

        if (estado == Incidente.Estado.CERRADO) {
            if (acciones == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para cerrar un incidente, describe las acciones tomadas");
            }
            LocalDateTime cierre = d.fechaCierre() != null ? d.fechaCierre()
                    : (i.getFechaCierre() != null ? i.getFechaCierre() : LocalDateTime.now());
            cierre = cierre.truncatedTo(ChronoUnit.SECONDS);
            if (cierre.isBefore(deteccion)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha de cierre no puede ser anterior a la fecha de detección");
            }
            if (cierre.isAfter(LocalDateTime.now().plusMinutes(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha de cierre no puede estar en el futuro");
            }
            i.setFechaCierre(cierre);
        } else {
            i.setFechaCierre(null);   // abierto o reabierto: sin fecha de cierre
        }

        i.setTitulo(d.titulo().trim());
        i.setTipo(d.tipo());
        i.setDescripcion(Textos.limpio(d.descripcion()));
        i.setActivo(activo);
        i.setSeveridad(d.severidad());
        i.setEstado(estado);
        i.setFechaDeteccion(deteccion);
        i.setAcciones(acciones);
        i.setResponsableId(d.responsableId());
    }
}

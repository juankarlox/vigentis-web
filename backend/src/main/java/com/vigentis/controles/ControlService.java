package com.vigentis.controles;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.comun.Textos;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import com.vigentis.riesgos.RiesgoBitacora;
import com.vigentis.riesgos.RiesgoBitacoraRepository;
import com.vigentis.riesgos.RiesgoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ControlService {

    public record Resumen(long total, long aplican, long excluidos, long implementados,
                          long enProgreso, long noImplementados, int porcentajeImplementacion) {}

    private final ControlRepository repo;
    private final RiesgoRepository riesgos;
    private final RiesgoBitacoraRepository bitacora;
    private final HistorialService historial;

    public ControlService(ControlRepository repo, RiesgoRepository riesgos,
                          RiesgoBitacoraRepository bitacora, HistorialService historial) {
        this.repo = repo;
        this.riesgos = riesgos;
        this.bitacora = bitacora;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public List<Control> buscar(String texto, Control.Tema tema, Control.Estado estado, Boolean aplica) {
        return repo.buscar(Textos.limpio(texto), tema, estado, aplica);
    }

    @Transactional(readOnly = true)
    public Control obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el control con id " + id));
    }

    @Transactional
    public Control actualizar(Long id, ControlRequest d) {
        Control c = obtener(id);
        String justificacion = Textos.limpio(d.justificacion());
        if (!d.aplica() && justificacion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Si el control no aplica, debes justificar la exclusión");
        }
        c.setAplica(d.aplica());
        c.setJustificacion(justificacion);
        if (d.estado() != null) c.setEstado(d.estado());
        c.setResponsableId(d.responsableId());
        repo.saveAndFlush(c);
        historial.registrar("CONTROL", c.getId(), c.getCodigoIso(), Historial.Accion.EDITAR,
                "Control " + c.getCodigoIso() + ": " + (c.getAplica() ? "aplica" : "no aplica")
                + ", estado " + c.getEstado());
        return c;
    }

    // Resumen de la Declaración de Aplicabilidad (para el dashboard)
    @Transactional(readOnly = true)
    public Resumen resumen() {
        List<Control> todos = repo.findAll();
        long aplican = todos.stream().filter(Control::getAplica).count();
        long impl = todos.stream().filter(c -> c.getAplica() && c.getEstado() == Control.Estado.IMPLEMENTADO).count();
        long prog = todos.stream().filter(c -> c.getAplica() && c.getEstado() == Control.Estado.EN_PROGRESO).count();
        long noImpl = aplican - impl - prog;
        int porcentaje = aplican == 0 ? 0 : (int) Math.round(impl * 100.0 / aplican);
        return new Resumen(todos.size(), aplican, todos.size() - aplican, impl, prog, noImpl, porcentaje);
    }

    @Transactional(readOnly = true)
    public List<Control> controlesDeRiesgo(Long riesgoId) {
        validarRiesgo(riesgoId);
        return repo.buscarPorRiesgo(riesgoId);
    }

    @Transactional
    public Control vincular(Long riesgoId, Long controlId) {
        validarRiesgo(riesgoId);
        Control c = obtener(controlId);
        if (!c.getAplica()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El control " + c.getCodigoIso() + " está excluido de la Declaración de Aplicabilidad");
        }
        if (repo.contarVinculo(riesgoId, controlId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese control ya está vinculado al riesgo");
        }
        repo.vincular(riesgoId, controlId);
        anotar(riesgoId, "Control " + c.getCodigoIso() + " vinculado: " + c.getNombre());
        return c;
    }

    @Transactional
    public void desvincular(Long riesgoId, Long controlId) {
        validarRiesgo(riesgoId);
        Control c = obtener(controlId);
        if (repo.desvincular(riesgoId, controlId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese control no estaba vinculado al riesgo");
        }
        anotar(riesgoId, "Control " + c.getCodigoIso() + " desvinculado");
    }

    private void validarRiesgo(Long riesgoId) {
        if (riesgos.findById(riesgoId).filter(EmpresaActual::esDeLaEmpresa).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el riesgo con id " + riesgoId);
        }
    }

    private void anotar(Long riesgoId, String texto) {
        bitacora.save(new RiesgoBitacora(riesgoId, RiesgoBitacora.Accion.TRATAMIENTO,
                                         null, null, null, null, texto));
    }
}

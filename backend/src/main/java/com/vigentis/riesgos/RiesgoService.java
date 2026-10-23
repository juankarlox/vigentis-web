package com.vigentis.riesgos;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.activos.Activo;
import com.vigentis.activos.ActivoRepository;
import com.vigentis.comun.Textos;
import com.vigentis.comun.UsuarioActual;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import com.vigentis.riesgos.RiesgoBitacora.Accion;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RiesgoService {

    private final RiesgoRepository repo;
    private final RiesgoBitacoraRepository bitacora;
    private final ActivoRepository activos;
    private final HistorialService historial;
    private final CriteriosService criterios;
    private final EntityManager em;

    public RiesgoService(RiesgoRepository repo, RiesgoBitacoraRepository bitacora, ActivoRepository activos,
                         HistorialService historial, CriteriosService criterios, EntityManager em) {
        this.em = em;
        this.repo = repo;
        this.bitacora = bitacora;
        this.activos = activos;
        this.historial = historial;
        this.criterios = criterios;
    }

    @Transactional(readOnly = true)
    public List<Riesgo> buscar(String texto, Riesgo.Estado estado, Long activoId) {
        return repo.buscar(Textos.limpio(texto), estado, activoId);
    }

    @Transactional(readOnly = true)
    public Riesgo obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el riesgo con id " + id));
    }

    @Transactional
    public Riesgo crear(RiesgoRequest d) {
        if (d.estado() == Riesgo.Estado.ACEPTADO) throw usarAceptar();
        Riesgo r = new Riesgo();
        r.setCodigo(String.format("RSG-%03d", repo.ultimoNumero(EmpresaActual.id()) + 1));
        copiarDatos(d, r);
        if (r.getFechaRevision() == null) {
            r.setFechaRevision(LocalDate.now().plusMonths(criterios.mesesRevision()));
        }
        repo.saveAndFlush(r);
        int nivel = r.getProbabilidad() * r.getImpacto();
        registrar(r.getId(), Accion.CREADO, null, r.getEstado().name(), null, nivel, "Riesgo registrado");
        historial.registrar("RIESGO", r.getId(), r.getCodigo(), Historial.Accion.CREAR,
                "Riesgo creado: " + r.getNombre() + " (nivel " + nivel + " " + Riesgo.clasificar(nivel) + ")");
        return r;
    }

    @Transactional
    public Riesgo actualizar(Long id, RiesgoRequest d) {
        Riesgo r = obtener(id);

        // Aceptar un riesgo es una aprobación formal: se hace con la acción "aceptar", no editando
        if (d.estado() == Riesgo.Estado.ACEPTADO && r.getEstado() != Riesgo.Estado.ACEPTADO) {
            throw usarAceptar();
        }

        // Cómo estaba antes del cambio
        int nivelAntes = r.getProbabilidad() * r.getImpacto();
        Riesgo.Estado estadoAntes = r.getEstado();
        Riesgo.Opcion opcionAntes = r.getOpcionTratamiento();
        String tratamientoAntes = r.getTratamiento();
        Integer residualAntes = producto(r.getProbabilidadResidual(), r.getImpactoResidual());

        copiarDatos(d, r);

        Integer residualDespues = producto(r.getProbabilidadResidual(), r.getImpactoResidual());
        int nivelDespues = r.getProbabilidad() * r.getImpacto();
        boolean cambioValoracion = nivelAntes != nivelDespues || !Objects.equals(residualAntes, residualDespues);

        // Si deja de estar aceptado, o si cambia la valoración de un riesgo aceptado, la aceptación ya no vale
        if (estadoAntes == Riesgo.Estado.ACEPTADO && (r.getEstado() != Riesgo.Estado.ACEPTADO || cambioValoracion)) {
            r.quitarAceptacion();
            if (r.getEstado() == Riesgo.Estado.ACEPTADO) r.setEstado(Riesgo.Estado.EN_TRATAMIENTO);
        }
        repo.saveAndFlush(r);

        if (nivelAntes != nivelDespues) {
            registrar(id, Accion.EVALUADO, null, null, nivelAntes, nivelDespues,
                      "Nueva valoración: P" + r.getProbabilidad() + " x I" + r.getImpacto());
        }
        if (estadoAntes != r.getEstado()) {
            registrar(id, Accion.CAMBIO_ESTADO, estadoAntes.name(), r.getEstado().name(), null, null,
                      estadoAntes == Riesgo.Estado.ACEPTADO ? "La aceptación anterior quedó sin efecto" : null);
        }
        if (opcionAntes != r.getOpcionTratamiento()
                || !Objects.equals(tratamientoAntes, r.getTratamiento())
                || !Objects.equals(residualAntes, residualDespues)) {
            String opcion = r.getOpcionTratamiento() == null ? "sin definir" : r.getOpcionTratamiento().name();
            registrar(id, Accion.TRATAMIENTO, null, null, residualAntes, residualDespues, "Tratamiento: " + opcion);
        }
        historial.registrar("RIESGO", r.getId(), r.getCodigo(), Historial.Accion.EDITAR,
                "Riesgo actualizado: " + r.getNombre() + " (estado " + r.getEstado() + ", nivel " + nivelDespues + ")");
        return r;
    }

    /**
     * Aceptación formal del riesgo por su dueño (ISO/IEC 27001, 6.1.3 f).
     * Si el nivel actual supera el nivel aceptable de los criterios, la justificación es obligatoria.
     */
    @Transactional
    public Riesgo aceptar(Long id, String justificacion) {
        Riesgo r = obtener(id);
        if (r.getEstado() == Riesgo.Estado.CERRADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un riesgo cerrado no se puede aceptar");
        }
        if (r.getEstado() == Riesgo.Estado.ACEPTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El riesgo ya está aceptado");
        }
        String texto = Textos.limpio(justificacion);
        Integer nivel = r.getNivelActual();
        boolean excepcional = !r.isDentroDeNivelAceptable();
        int aceptable = NivelesRiesgo.nivelAceptable(r.empresa());
        if (excepcional && texto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nivel actual (" + nivel + ") supera el nivel aceptable (" + aceptable
                    + "). Para aceptarlo debes justificar la decisión");
        }
        Riesgo.Estado antes = r.getEstado();
        r.registrarAceptacion(UsuarioActual.id(), texto);
        repo.saveAndFlush(r);
        em.refresh(r);   // para devolver también el nombre de quien aceptó
        registrar(id, Accion.ACEPTACION, antes.name(), Riesgo.Estado.ACEPTADO.name(), null, nivel,
                (excepcional ? "Aceptación EXCEPCIONAL (supera el nivel aceptable " + aceptable + ")"
                             : "Riesgo aceptado dentro del nivel aceptable")
                + (texto != null ? ". Justificación: " + texto : ""));
        historial.registrar("RIESGO", r.getId(), r.getCodigo(), Historial.Accion.EDITAR,
                "Riesgo aceptado formalmente con nivel " + nivel + (excepcional ? " (excepcional)" : ""));
        return r;
    }

    /** Revisión periódica: deja constancia y programa la siguiente (ISO/IEC 27001, 8.2). */
    @Transactional
    public Riesgo revisar(Long id, String comentario) {
        Riesgo r = obtener(id);
        if (r.getEstado() == Riesgo.Estado.CERRADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un riesgo cerrado no necesita revisión");
        }
        LocalDate siguiente = LocalDate.now().plusMonths(criterios.mesesRevision());
        r.setFechaRevision(siguiente);
        repo.saveAndFlush(r);
        String texto = Textos.limpio(comentario);
        registrar(id, Accion.REVISION, null, null, null, r.getNivelActual(),
                "Revisión periódica realizada. Próxima: " + siguiente + (texto != null ? ". " + texto : ""));
        historial.registrar("RIESGO", r.getId(), r.getCodigo(), Historial.Accion.EDITAR,
                "Revisión periódica del riesgo; próxima el " + siguiente);
        return r;
    }

    @Transactional
    public RiesgoBitacora comentar(Long id, String comentario) {
        obtener(id);
        return registrar(id, Accion.COMENTARIO, null, null, null, null, comentario.trim());
    }

    @Transactional(readOnly = true)
    public List<RiesgoBitacora> verBitacora(Long id) {
        obtener(id);
        return bitacora.findByRiesgoIdOrderByFechaDescIdDesc(id);
    }

    private RiesgoBitacora registrar(Long riesgoId, Accion accion, String estadoAnterior, String estadoNuevo,
                                     Integer nivelAnterior, Integer nivelNuevo, String comentario) {
        return bitacora.saveAndFlush(new RiesgoBitacora(riesgoId, accion, estadoAnterior, estadoNuevo,
                                                        nivelAnterior, nivelNuevo, comentario));
    }

    private void copiarDatos(RiesgoRequest d, Riesgo r) {
        if ((d.probabilidadResidual() == null) != (d.impactoResidual() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para el riesgo residual se necesitan probabilidad e impacto juntos");
        }
        Activo activo = null;
        if (d.activoId() != null) {
            activo = activos.findById(d.activoId()).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el activo con id " + d.activoId()));
        }
        r.setNombre(d.nombre().trim());
        r.setDescripcion(Textos.limpio(d.descripcion()));
        r.setAmenaza(Textos.limpio(d.amenaza()));
        r.setVulnerabilidad(Textos.limpio(d.vulnerabilidad()));
        r.setActivo(activo);
        r.setProbabilidad(d.probabilidad());
        r.setImpacto(d.impacto());
        r.setOpcionTratamiento(d.opcionTratamiento());
        r.setTratamiento(Textos.limpio(d.tratamiento()));
        r.setProbabilidadResidual(d.probabilidadResidual());
        r.setImpactoResidual(d.impactoResidual());
        if (d.estado() != null) r.setEstado(d.estado());
        r.setResponsableId(d.responsableId());
        if (d.fechaRevision() != null) r.setFechaRevision(d.fechaRevision());
    }

    private static Integer producto(Integer a, Integer b) {
        return (a == null || b == null) ? null : a * b;
    }

    private static ResponseStatusException usarAceptar() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Para aceptar un riesgo usa la acción \"Aceptar riesgo\": queda registrado quién lo aprobó y cuándo");
    }
}

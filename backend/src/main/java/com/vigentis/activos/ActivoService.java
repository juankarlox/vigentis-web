package com.vigentis.activos;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.comun.Textos;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivoService {

    private final ActivoRepository repo;
    private final HistorialService historial;

    public ActivoService(ActivoRepository repo, HistorialService historial) {
        this.repo = repo;
        this.historial = historial;
    }

    @Transactional(readOnly = true)
    public List<Activo> buscar(String texto, Activo.Tipo tipo, Activo.Estado estado) {
        return repo.buscar(Textos.limpio(texto), tipo, estado);
    }

    @Transactional(readOnly = true)
    public Activo obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el activo con id " + id));
    }

    @Transactional
    public Activo crear(ActivoRequest datos) {
        Activo activo = new Activo();
        activo.setCodigo(String.format("ACT-%03d", repo.ultimoNumero(EmpresaActual.id()) + 1));
        copiarDatos(datos, activo);
        repo.saveAndFlush(activo);
        historial.registrar("ACTIVO", activo.getId(), activo.getCodigo(), Historial.Accion.CREAR,
                "Activo creado: " + activo.getNombre() + " (criticidad " + activo.getCriticidad() + ")");
        return activo;
    }

    @Transactional
    public Activo actualizar(Long id, ActivoRequest datos) {
        Activo activo = obtener(id);
        copiarDatos(datos, activo);
        repo.saveAndFlush(activo);
        historial.registrar("ACTIVO", activo.getId(), activo.getCodigo(), Historial.Accion.EDITAR,
                "Activo actualizado: " + activo.getNombre() + " (estado " + activo.getEstado()
                + ", criticidad " + activo.getCriticidad() + ")");
        return activo;
    }

    @Transactional
    public void eliminar(Long id) {
        Activo activo = obtener(id);
        repo.delete(activo);
        historial.registrar("ACTIVO", activo.getId(), activo.getCodigo(), Historial.Accion.ELIMINAR,
                "Activo eliminado: " + activo.getNombre());
    }

    private void copiarDatos(ActivoRequest d, Activo a) {
        a.setNombre(d.nombre().trim());
        a.setTipo(d.tipo());
        a.setConfidencialidad(d.confidencialidad());
        a.setIntegridad(d.integridad());
        a.setDisponibilidad(d.disponibilidad());
        if (d.estado() != null) a.setEstado(d.estado());
        a.setDescripcion(Textos.limpio(d.descripcion()));
        a.setResponsableId(d.responsableId());
    }
}

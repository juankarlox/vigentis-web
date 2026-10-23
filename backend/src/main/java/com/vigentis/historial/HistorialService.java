package com.vigentis.historial;

import com.vigentis.comun.Textos;
import com.vigentis.comun.UsuarioActual;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistorialService {

    private final HistorialRepository repo;

    public HistorialService(HistorialRepository repo) {
        this.repo = repo;
    }

    /** Anota una acción. Se llama desde los demás servicios, dentro de su misma transacción. */
    @Transactional
    public void registrar(String entidad, Long entidadId, String entidadCodigo,
                          Historial.Accion accion, String detalle) {
        repo.save(new Historial(UsuarioActual.id(), entidad, entidadId, entidadCodigo, accion,
                                Textos.recortar(detalle, 500)));
    }

    /** Igual que registrar, pero indicando empresa y usuario (login y registro, cuando aún no hay sesión). */
    @Transactional
    public void registrarComo(Long empresaId, Long usuarioId, String entidad, Long entidadId, String entidadCodigo,
                              Historial.Accion accion, String detalle) {
        Historial h = new Historial(usuarioId, entidad, entidadId, entidadCodigo, accion, Textos.recortar(detalle, 500));
        h.asignarEmpresa(empresaId);
        repo.save(h);
    }

    @Transactional(readOnly = true)
    public List<Historial> buscar(String entidad, Historial.Accion accion, String texto,
                                  LocalDate desde, LocalDate hasta, Integer limite) {
        int max = (limite == null || limite < 1) ? 200 : Math.min(limite, 1000);
        return repo.buscar(
                Textos.limpio(entidad) == null ? null : entidad.trim().toUpperCase(),
                accion,
                Textos.limpio(texto),
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                PageRequest.of(0, max));
    }

    @Transactional(readOnly = true)
    public List<Historial> recientes() {
        return repo.findTop10ByOrderByIdDesc();
    }
}

package com.vigentis.riesgos;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.comun.UsuarioActual;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CriteriosService {

    /** Descripciones por defecto de la escala 1 a 5 (cada empresa las puede cambiar). */
    public static final List<String> ESCALA_PROBABILIDAD = List.of(
            "Rara: casi imposible que ocurra (menos de una vez en 5 años)",
            "Improbable: podría ocurrir alguna vez (una vez en 2 a 5 años)",
            "Posible: puede ocurrir (una vez al año)",
            "Probable: ocurre con frecuencia (varias veces al año)",
            "Casi segura: se espera que ocurra (mensual o más)");

    public static final List<String> ESCALA_IMPACTO = List.of(
            "Insignificante: sin efecto apreciable en la operación ni en los datos",
            "Menor: molestias internas, se corrige en horas",
            "Moderado: afecta un proceso o datos internos, se corrige en días",
            "Mayor: afecta clientes o datos personales, pérdidas económicas o legales",
            "Catastrófico: detiene la operación o expone información crítica");

    private final CriteriosRiesgoRepository repo;
    private final HistorialService historial;

    public CriteriosService(CriteriosRiesgoRepository repo, HistorialService historial,
                            PlatformTransactionManager transacciones) {
        this.repo = repo;
        this.historial = historial;
        TransactionTemplate lectura = new TransactionTemplate(transacciones);
        lectura.setReadOnly(true);
        // Cómo leer los umbrales de una empresa cuando todavía no están en memoria
        NivelesRiesgo.configurarCargador(empresaId -> EmpresaActual.como(empresaId, () -> lectura.execute(estado ->
                repo.findFirstByOrderByIdAsc()
                        .map(c -> new NivelesRiesgo.Umbrales(c.getUmbralMedio(), c.getUmbralAlto(),
                                                             c.getUmbralCritico(), c.getNivelAceptable()))
                        .orElse(NivelesRiesgo.POR_DEFECTO))));
    }

    @Transactional(readOnly = true)
    public CriteriosRiesgo obtener() {
        return repo.findFirstByOrderByIdAsc().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tu empresa no tiene criterios de riesgo configurados"));
    }

    @Transactional(readOnly = true)
    public int mesesRevision() {
        return repo.findFirstByOrderByIdAsc().map(CriteriosRiesgo::getMesesRevision).orElse(6);
    }

    @Transactional
    public CriteriosRiesgo actualizar(CriteriosRequest d) {
        if (!(d.umbralMedio() < d.umbralAlto() && d.umbralAlto() < d.umbralCritico())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los umbrales deben ir en orden: medio < alto < crítico");
        }
        CriteriosRiesgo c = obtener();
        c.setUmbralMedio(d.umbralMedio());
        c.setUmbralAlto(d.umbralAlto());
        c.setUmbralCritico(d.umbralCritico());
        c.setNivelAceptable(d.nivelAceptable());
        c.setMesesRevision(d.mesesRevision());
        if (d.escalaProbabilidad() != null) c.setEscalaProbabilidad(d.escalaProbabilidad());
        if (d.escalaImpacto() != null) c.setEscalaImpacto(d.escalaImpacto());
        c.setActualizadoPor(UsuarioActual.id());
        repo.saveAndFlush(c);
        NivelesRiesgo.olvidar(c.empresa());
        historial.registrar("CRITERIOS", c.getId(), null, Historial.Accion.EDITAR,
                "Criterios de riesgo: medio desde " + c.getUmbralMedio() + ", alto desde " + c.getUmbralAlto()
                + ", crítico desde " + c.getUmbralCritico() + ", aceptable hasta " + c.getNivelAceptable()
                + ", revisión cada " + c.getMesesRevision() + " meses");
        return c;
    }
}

package com.vigentis.riesgos;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Criterios de evaluación y aceptación de riesgos de la organización (ISO/IEC 27001, 6.1.2 a). */
@Entity
@Table(name = "criterios_riesgo")
public class CriteriosRiesgo implements DeEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "umbral_medio", nullable = false, columnDefinition = "TINYINT")
    private Integer umbralMedio;

    @Column(name = "umbral_alto", nullable = false, columnDefinition = "TINYINT")
    private Integer umbralAlto;

    @Column(name = "umbral_critico", nullable = false, columnDefinition = "TINYINT")
    private Integer umbralCritico;

    @Column(name = "nivel_aceptable", nullable = false, columnDefinition = "TINYINT")
    private Integer nivelAceptable;

    @Column(name = "meses_revision", nullable = false, columnDefinition = "TINYINT")
    private Integer mesesRevision;

    // Una línea por cada valor de la escala (1 a 5)
    @Column(name = "escala_probabilidad", columnDefinition = "TEXT")
    private String escalaProbabilidad;

    @Column(name = "escala_impacto", columnDefinition = "TEXT")
    private String escalaImpacto;

    @Column(name = "actualizado_por")
    private Long actualizadoPor;

    @Formula("(SELECT u.nombre FROM usuarios u WHERE u.id = actualizado_por)")
    private String actualizadoPorNombre;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    private static List<String> lineas(String texto) {
        return texto == null ? List.of() : Arrays.stream(texto.split("\\R")).map(String::trim).toList();
    }

    public Long getId() { return id; }
    public Integer getUmbralMedio() { return umbralMedio; }
    public void setUmbralMedio(Integer v) { this.umbralMedio = v; }
    public Integer getUmbralAlto() { return umbralAlto; }
    public void setUmbralAlto(Integer v) { this.umbralAlto = v; }
    public Integer getUmbralCritico() { return umbralCritico; }
    public void setUmbralCritico(Integer v) { this.umbralCritico = v; }
    public Integer getNivelAceptable() { return nivelAceptable; }
    public void setNivelAceptable(Integer v) { this.nivelAceptable = v; }
    public Integer getMesesRevision() { return mesesRevision; }
    public void setMesesRevision(Integer v) { this.mesesRevision = v; }
    public List<String> getEscalaProbabilidad() { return lineas(escalaProbabilidad); }
    public void setEscalaProbabilidad(List<String> v) { this.escalaProbabilidad = v == null ? null : String.join("\n", v); }
    public List<String> getEscalaImpacto() { return lineas(escalaImpacto); }
    public void setEscalaImpacto(List<String> v) { this.escalaImpacto = v == null ? null : String.join("\n", v); }
    public Long getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(Long v) { this.actualizadoPor = v; }
    public String getActualizadoPorNombre() { return actualizadoPorNombre; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

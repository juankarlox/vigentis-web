package com.vigentis.controles;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "controles")
public class Control implements DeEmpresa {

    public enum Tema { ORGANIZACIONAL, PERSONAS, FISICO, TECNOLOGICO }
    public enum Estado { NO_IMPLEMENTADO, EN_PROGRESO, IMPLEMENTADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "codigo_iso", nullable = false, length = 10, updatable = false)
    private String codigoIso;

    @Column(nullable = false, length = 200, updatable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private Tema tema;

    @Column(nullable = false)
    private Boolean aplica = true;

    @Column(columnDefinition = "TEXT")
    private String justificacion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.NO_IMPLEMENTADO;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public String getCodigoIso() { return codigoIso; }
    public String getNombre() { return nombre; }
    public Tema getTema() { return tema; }
    public Boolean getAplica() { return aplica; }
    public void setAplica(Boolean aplica) { this.aplica = aplica; }
    public String getJustificacion() { return justificacion; }
    public void setJustificacion(String justificacion) { this.justificacion = justificacion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

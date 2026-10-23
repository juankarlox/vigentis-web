package com.vigentis.auditorias;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import com.vigentis.controles.Control;
import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hallazgos")
public class Hallazgo implements DeEmpresa {

    public enum Tipo { NC_MAYOR, NC_MENOR, OBSERVACION, OPORTUNIDAD_MEJORA }
    public enum Estado { ABIERTO, EN_PROCESO, CERRADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "auditoria_id", nullable = false, updatable = false)
    private Long auditoriaId;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "control_id")
    private Control control;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ABIERTO;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "accion_correctiva", columnDefinition = "TEXT")
    private String accionCorrectiva;

    /** true si sigue abierto y ya pasó la fecha límite. */
    public boolean isVencido() {
        return estado != Estado.CERRADO && fechaLimite != null && fechaLimite.isBefore(LocalDate.now());
    }

    public Long getId() { return id; }
    public Long getAuditoriaId() { return auditoriaId; }
    public void setAuditoriaId(Long auditoriaId) { this.auditoriaId = auditoriaId; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public String getAccionCorrectiva() { return accionCorrectiva; }
    public void setAccionCorrectiva(String a) { this.accionCorrectiva = a; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

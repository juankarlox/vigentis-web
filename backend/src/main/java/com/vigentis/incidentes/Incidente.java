package com.vigentis.incidentes;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import com.vigentis.activos.Activo;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incidentes")
public class Incidente implements DeEmpresa {

    public enum Tipo { PHISHING, MALWARE, ACCESO_NO_AUTORIZADO, FUGA_DE_DATOS, CAIDA_DE_SERVICIO, OTRO }
    public enum Severidad { BAJA, MEDIA, ALTA, CRITICA }
    public enum Estado { ABIERTO, EN_ATENCION, CERRADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activo_id")
    private Activo activo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Severidad severidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ABIERTO;

    @Column(name = "fecha_deteccion", nullable = false)
    private LocalDateTime fechaDeteccion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(columnDefinition = "TEXT")
    private String acciones;

    @Column(name = "reportado_por", updatable = false)
    private Long reportadoPor;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Generated
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }
    public Severidad getSeveridad() { return severidad; }
    public void setSeveridad(Severidad severidad) { this.severidad = severidad; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDateTime getFechaDeteccion() { return fechaDeteccion; }
    public void setFechaDeteccion(LocalDateTime f) { this.fechaDeteccion = f; }
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime f) { this.fechaCierre = f; }
    public String getAcciones() { return acciones; }
    public void setAcciones(String acciones) { this.acciones = acciones; }
    public Long getReportadoPor() { return reportadoPor; }
    public void setReportadoPor(Long reportadoPor) { this.reportadoPor = reportadoPor; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

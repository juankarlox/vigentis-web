package com.vigentis.activos;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "activos")
public class Activo implements DeEmpresa {

    public enum Tipo { SERVIDOR, SOFTWARE, EQUIPO, RED }
    public enum Estado { ACTIVO, EN_MANTENIMIENTO, DADO_DE_BAJA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer confidencialidad;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer integridad;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer disponibilidad;

    // La calcula MySQL con el mayor valor de C, I y D
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false, length = 10)
    private String criticidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

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
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Integer getConfidencialidad() { return confidencialidad; }
    public void setConfidencialidad(Integer c) { this.confidencialidad = c; }
    public Integer getIntegridad() { return integridad; }
    public void setIntegridad(Integer i) { this.integridad = i; }
    public Integer getDisponibilidad() { return disponibilidad; }
    public void setDisponibilidad(Integer d) { this.disponibilidad = d; }
    public String getCriticidad() { return criticidad; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

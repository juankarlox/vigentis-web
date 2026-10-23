package com.vigentis.historial;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Registro general de quién hizo qué y cuándo, en todos los módulos. */
@Entity
@Table(name = "historial")
public class Historial implements DeEmpresa {

    public enum Accion { CREAR, EDITAR, ELIMINAR, IMPORTAR, INICIAR_SESION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Generated
    @Column(insertable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Formula("(SELECT u.nombre FROM usuarios u WHERE u.id = usuario_id)")
    private String usuarioNombre;

    @Column(nullable = false, length = 30, updatable = false)
    private String entidad;

    @Column(name = "entidad_id", updatable = false)
    private Long entidadId;

    @Column(name = "entidad_codigo", length = 20, updatable = false)
    private String entidadCodigo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private Accion accion;

    @Column(length = 500, updatable = false)
    private String detalle;

    protected Historial() {}

    public Historial(Long usuarioId, String entidad, Long entidadId, String entidadCodigo,
                     Accion accion, String detalle) {
        this.usuarioId = usuarioId;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.entidadCodigo = entidadCodigo;
        this.accion = accion;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public String getEntidad() { return entidad; }
    public Long getEntidadId() { return entidadId; }
    public String getEntidadCodigo() { return entidadCodigo; }
    public Accion getAccion() { return accion; }
    public String getDetalle() { return detalle; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

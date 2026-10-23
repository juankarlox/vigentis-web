package com.vigentis.riesgos;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import com.vigentis.comun.UsuarioActual;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "riesgo_bitacora")
public class RiesgoBitacora implements DeEmpresa {

    public enum Accion { CREADO, EVALUADO, CAMBIO_ESTADO, TRATAMIENTO, COMENTARIO, ACEPTACION, REVISION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(name = "riesgo_id", nullable = false, updatable = false)
    private Long riesgoId;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Formula("(SELECT u.nombre FROM usuarios u WHERE u.id = usuario_id)")
    private String usuarioNombre;

    @Generated
    @Column(insertable = false, updatable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30, updatable = false)
    private Accion accion;

    @Column(name = "estado_anterior", length = 20, updatable = false)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 20, updatable = false)
    private String estadoNuevo;

    @Column(name = "nivel_anterior", columnDefinition = "TINYINT", updatable = false)
    private Integer nivelAnterior;

    @Column(name = "nivel_nuevo", columnDefinition = "TINYINT", updatable = false)
    private Integer nivelNuevo;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String comentario;

    protected RiesgoBitacora() {}

    public RiesgoBitacora(Long riesgoId, Accion accion, String estadoAnterior, String estadoNuevo,
                          Integer nivelAnterior, Integer nivelNuevo, String comentario) {
        this.riesgoId = riesgoId;
        this.usuarioId = UsuarioActual.id();
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.nivelAnterior = nivelAnterior;
        this.nivelNuevo = nivelNuevo;
        this.comentario = comentario;
    }

    public Long getId() { return id; }
    public Long getRiesgoId() { return riesgoId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public LocalDateTime getFecha() { return fecha; }
    public Accion getAccion() { return accion; }
    public String getEstadoAnterior() { return estadoAnterior; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public Integer getNivelAnterior() { return nivelAnterior; }
    public Integer getNivelNuevo() { return nivelNuevo; }
    public String getComentario() { return comentario; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

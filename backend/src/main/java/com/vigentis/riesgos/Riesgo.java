package com.vigentis.riesgos;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import com.vigentis.activos.Activo;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "riesgos")
public class Riesgo implements DeEmpresa {

    public enum Estado { ABIERTO, EN_TRATAMIENTO, MITIGADO, ACEPTADO, CERRADO }
    public enum Opcion { MITIGAR, ACEPTAR, TRANSFERIR, EVITAR }

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
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Qué podría pasar (ej.: phishing) y qué debilidad lo permite (ej.: sin doble factor)
    @Column(length = 200)
    private String amenaza;

    @Column(length = 200)
    private String vulnerabilidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activo_id")
    private Activo activo;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer probabilidad;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer impacto;

    // probabilidad x impacto, lo calcula MySQL
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "nivel_valor", columnDefinition = "TINYINT", insertable = false, updatable = false)
    private Integer nivelValor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "opcion_tratamiento", length = 20)
    private Opcion opcionTratamiento;

    @Column(columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "probabilidad_residual", columnDefinition = "TINYINT")
    private Integer probabilidadResidual;

    @Column(name = "impacto_residual", columnDefinition = "TINYINT")
    private Integer impactoResidual;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "nivel_residual", columnDefinition = "TINYINT", insertable = false, updatable = false)
    private Integer nivelResidual;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ABIERTO;

    @Column(name = "responsable_id")
    private Long responsableId;

    // Revisión periódica (ISO 8.2)
    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    // Aceptación formal del riesgo residual (ISO 6.1.3 f)
    @Column(name = "aceptado_por")
    private Long aceptadoPor;

    @Formula("(SELECT u.nombre FROM usuarios u WHERE u.id = aceptado_por)")
    private String aceptadoPorNombre;

    @Column(name = "aceptado_en")
    private LocalDateTime aceptadoEn;

    @Column(name = "justificacion_aceptacion", columnDefinition = "TEXT")
    private String justificacionAceptacion;

    @Generated
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    /** Clasificación según los criterios definidos por la organización. */
    public static String clasificar(Integer valor) {
        return NivelesRiesgo.clasificar(valor);
    }

    public String getNivel() { return NivelesRiesgo.clasificar(empresaId, nivelValor); }
    public String getNivelResidualTexto() { return NivelesRiesgo.clasificar(empresaId, nivelResidual); }

    /** El nivel que cuenta hoy: el residual si ya se evaluó, si no el inherente. */
    public Integer getNivelActual() { return nivelResidual != null ? nivelResidual : nivelValor; }

    /** true si el nivel actual está dentro del nivel aceptable definido en los criterios. */
    public boolean isDentroDeNivelAceptable() { return NivelesRiesgo.esAceptable(empresaId, getNivelActual()); }

    /** true si la fecha de revisión ya pasó y el riesgo no está cerrado. */
    public boolean isRevisionVencida() {
        return estado != Estado.CERRADO && fechaRevision != null && fechaRevision.isBefore(LocalDate.now());
    }

    public void registrarAceptacion(Long usuarioId, String justificacion) {
        this.estado = Estado.ACEPTADO;
        this.aceptadoPor = usuarioId;
        this.aceptadoEn = LocalDateTime.now().withNano(0);
        this.justificacionAceptacion = justificacion;
    }

    public void quitarAceptacion() {
        this.aceptadoPor = null;
        this.aceptadoEn = null;
        this.justificacionAceptacion = null;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getAmenaza() { return amenaza; }
    public void setAmenaza(String amenaza) { this.amenaza = amenaza; }
    public String getVulnerabilidad() { return vulnerabilidad; }
    public void setVulnerabilidad(String vulnerabilidad) { this.vulnerabilidad = vulnerabilidad; }
    public Activo getActivo() { return activo; }
    public void setActivo(Activo activo) { this.activo = activo; }
    public Integer getProbabilidad() { return probabilidad; }
    public void setProbabilidad(Integer probabilidad) { this.probabilidad = probabilidad; }
    public Integer getImpacto() { return impacto; }
    public void setImpacto(Integer impacto) { this.impacto = impacto; }
    public Integer getNivelValor() { return nivelValor; }
    public Opcion getOpcionTratamiento() { return opcionTratamiento; }
    public void setOpcionTratamiento(Opcion o) { this.opcionTratamiento = o; }
    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }
    public Integer getProbabilidadResidual() { return probabilidadResidual; }
    public void setProbabilidadResidual(Integer p) { this.probabilidadResidual = p; }
    public Integer getImpactoResidual() { return impactoResidual; }
    public void setImpactoResidual(Integer i) { this.impactoResidual = i; }
    public Integer getNivelResidual() { return nivelResidual; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }
    public LocalDate getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDate fechaRevision) { this.fechaRevision = fechaRevision; }
    public Long getAceptadoPor() { return aceptadoPor; }
    public String getAceptadoPorNombre() { return aceptadoPorNombre; }
    public LocalDateTime getAceptadoEn() { return aceptadoEn; }
    public String getJustificacionAceptacion() { return justificacionAceptacion; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

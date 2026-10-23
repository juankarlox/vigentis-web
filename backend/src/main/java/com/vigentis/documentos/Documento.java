package com.vigentis.documentos;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/** Políticas, procedimientos, registros y evidencias del SGSI (ISO/IEC 27001, 7.5). */
@Entity
@Table(name = "documentos")
public class Documento implements DeEmpresa {

    public enum Tipo { POLITICA, PROCEDIMIENTO, REGISTRO, EVIDENCIA, OTRO }
    public enum Estado { BORRADOR, APROBADO, OBSOLETO }

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
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, length = 10)
    private String version = "1.0";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.BORRADOR;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "archivo_nombre", length = 255)
    private String archivoNombre;

    // Ruta interna en el servidor: no tiene getter para que nunca se envíe a la web
    @Column(name = "archivo_ruta", length = 500)
    private String archivoRuta;

    @Column(name = "archivo_tipo", length = 100)
    private String archivoTipo;

    @Column(name = "archivo_tamano")
    private Long archivoTamano;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    @Generated
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public boolean isTieneArchivo() { return archivoRuta != null; }

    /** true si está aprobado y su fecha de revisión ya llegó o llega en los próximos 30 días. */
    public boolean isRevisionPendiente() {
        return estado == Estado.APROBADO && fechaRevision != null
                && !fechaRevision.isAfter(LocalDate.now().plusDays(30));
    }

    String rutaInterna() { return archivoRuta; }
    void cambiarRutaInterna(String ruta) { this.archivoRuta = ruta; }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long responsableId) { this.responsableId = responsableId; }
    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String archivoNombre) { this.archivoNombre = archivoNombre; }
    public String getArchivoTipo() { return archivoTipo; }
    public void setArchivoTipo(String archivoTipo) { this.archivoTipo = archivoTipo; }
    public Long getArchivoTamano() { return archivoTamano; }
    public void setArchivoTamano(Long archivoTamano) { this.archivoTamano = archivoTamano; }
    public LocalDate getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(LocalDate f) { this.fechaAprobacion = f; }
    public LocalDate getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDate f) { this.fechaRevision = f; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

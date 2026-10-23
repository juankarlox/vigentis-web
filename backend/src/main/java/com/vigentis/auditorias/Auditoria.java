package com.vigentis.auditorias;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auditorias")
public class Auditoria implements DeEmpresa {

    public enum Tipo { INTERNA, EXTERNA }

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(name = "fecha_auditoria", nullable = false)
    private LocalDate fechaAuditoria;

    @Column(length = 150)
    private String auditor;

    @Column(name = "archivo_origen", length = 255)
    private String archivoOrigen;

    @Column(name = "importado_por", updatable = false)
    private Long importadoPor;

    @Generated
    @Column(name = "importado_en", insertable = false, updatable = false)
    private LocalDateTime importadoEn;

    // Contadores calculados por MySQL al consultar
    @Formula("(SELECT COUNT(*) FROM hallazgos h WHERE h.auditoria_id = id)")
    private Long totalHallazgos;

    @Formula("(SELECT COUNT(*) FROM hallazgos h WHERE h.auditoria_id = id AND h.estado <> 'CERRADO')")
    private Long hallazgosAbiertos;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public LocalDate getFechaAuditoria() { return fechaAuditoria; }
    public void setFechaAuditoria(LocalDate f) { this.fechaAuditoria = f; }
    public String getAuditor() { return auditor; }
    public void setAuditor(String auditor) { this.auditor = auditor; }
    public String getArchivoOrigen() { return archivoOrigen; }
    public void setArchivoOrigen(String archivoOrigen) { this.archivoOrigen = archivoOrigen; }
    public Long getImportadoPor() { return importadoPor; }
    public void setImportadoPor(Long importadoPor) { this.importadoPor = importadoPor; }
    public LocalDateTime getImportadoEn() { return importadoEn; }
    public Long getTotalHallazgos() { return totalHallazgos; }
    public Long getHallazgosAbiertos() { return hallazgosAbiertos; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

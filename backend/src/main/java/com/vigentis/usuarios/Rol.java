package com.vigentis.usuarios;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;

/** Rol editable por cada empresa: un nombre y los permisos que incluye. */
@Entity
@Table(name = "roles")
public class Rol implements DeEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    // El rol de administrador del sistema no se puede editar ni borrar
    @Column(name = "es_sistema", nullable = false, updatable = false)
    private Boolean esSistema = false;

    @Generated
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso",
               joinColumns = @JoinColumn(name = "rol_id"),
               inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new HashSet<>();

    @Formula("(SELECT COUNT(*) FROM usuarios u WHERE u.rol_id = id)")
    private Long totalUsuarios;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getEsSistema() { return esSistema; }
    public void marcarComoSistema() { this.esSistema = true; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public Set<Permiso> getPermisos() { return permisos; }
    public void setPermisos(Set<Permiso> permisos) { this.permisos = permisos; }
    public Long getTotalUsuarios() { return totalUsuarios; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

package com.vigentis.usuarios;

import com.vigentis.comun.DeEmpresa;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "usuarios")
public class Usuario implements DeEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Empresa dueña del registro: Hibernate la llena y filtra sola (multi-empresa)
    @TenantId
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    // Hash BCrypt. No tiene getter para que nunca se envíe a la web.
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Generated
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    // ----- Clave (métodos sin "get" para que no salgan en el JSON) -----
    public String hashClave() { return passwordHash; }
    public void asignarHashClave(String hash) { this.passwordHash = hash; }

    // ----- Bloqueo por intentos fallidos (ISO/IEC 27001, A.8.5) -----
    public boolean isBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    public void registrarFallo(int maximoIntentos, int minutosBloqueo) {
        intentosFallidos = intentosFallidos + 1;
        if (intentosFallidos >= maximoIntentos) {
            bloqueadoHasta = LocalDateTime.now().plusMinutes(minutosBloqueo);
            intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = LocalDateTime.now();
    }

    public void desbloquear() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public boolean esAdministradorSistema() {
        return rol != null && Boolean.TRUE.equals(rol.getEsSistema());
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public Integer getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    @Override
    public Long empresa() { return empresaId; }

    /** Solo para crear registros de una empresa nueva desde el sistema (registro). */
    public void asignarEmpresa(Long empresaId) { this.empresaId = empresaId; }
}

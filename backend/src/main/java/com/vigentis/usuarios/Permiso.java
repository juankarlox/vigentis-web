package com.vigentis.usuarios;

import jakarta.persistence.*;

/** Permisos fijos del sistema (los crea la migración V2; no se editan desde la web). */
@Entity
@Table(name = "permisos")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 30, updatable = false)
    private String modulo;

    @Column(nullable = false, length = 255, updatable = false)
    private String descripcion;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getModulo() { return modulo; }
    public String getDescripcion() { return descripcion; }
}

package com.vigentis.empresas;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;

/** Cada organización registrada en Vigentis. Sus datos están separados de los de las demás. */
@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 30)
    private String nit;

    @Column(length = 60)
    private String sector;

    @Column(nullable = false)
    private Boolean activa = true;

    @Generated
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public Boolean getActiva() { return activa; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}

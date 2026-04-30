package com.optimapc.backend.modelo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "premontados")
public class Premontado extends ConfiguracionPC {

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private String marca;

    private Integer descuento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSO sistemaOperativo;

    @Column(nullable = false)
    private Integer stock = 0;

    @ElementCollection
    @CollectionTable(name = "premontado_usos", joinColumns = @JoinColumn(name = "premontado_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "uso", nullable = false)
    private Set<TipoUso> usosPrevistos = new HashSet<>();

    private String imagenUrl;

    @Column(nullable = false)
    private Boolean esReacondicionado;

    @Transient
    private Double valoracionMedia() {
        if (valoraciones.isEmpty()) return 0.0;
        return valoraciones.stream().mapToInt(Valoracion::getPuntuacion).average().getAsDouble();
    }

    @Transient
    private Integer getNumeroValoraciones() {
        if (valoraciones.isEmpty()) return 0;
        return valoraciones.size();
    }

    @OneToMany(mappedBy = "premontado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Valoracion> valoraciones = new ArrayList<>();

    @Transient
    public Double getPrecioReducido() {
        if (descuento == null || descuento == 0) return null;
        return this.getPrecio() * (1 - descuento / 100.0);
    }
}

package com.optimapc.backend.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.optimapc.backend.usuario.Usuario;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "configuraciones_pc")
@Inheritance(strategy = InheritanceType.JOINED)
public class ConfiguracionPC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "nombre_configuracion")
    private String nombreConfiguracion;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }

    @ElementCollection
    @CollectionTable(name = "configuracion_usos_previstos", joinColumns = @JoinColumn(name = "configuracion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "uso")
    private Set<TipoUso> usosPrevistos = new HashSet<>();

    @Transient
    private Boolean favorita;

    @Transient
    private Double indicadorEquilibrio; // se basará en rendimientoScore para mostrar un "equilibrio" entre componentes, que no estén descompensados (ej: CPU muy potente y GPU muy floja).

    @Transient
    private Double rendimientoScore; // se calcula en el servicio de rendimiento basado en una serie de reglas entre componentes

    @Transient
    private Double rendimientoPorEuro; // se normaliza el rendimientoScore dividiéndolo por el precio y comparándolo con maximo y mínimo

    @Transient
    public Double getPrecio() {
        return componentes.stream()
                .filter(c -> c.getComponente() != null)
                .mapToDouble(c -> c.getComponente().getPrecio() * c.getCantidad())
                .sum();
    }

    // Precio real para el cálculo de rendimiento/€. Las subclases pueden sobreescribirlo
    // para aplicar descuentos u otros ajustes (ej. Premontado con precioReducido).
    @Transient
    public Double getPrecioEfectivo() {
        return getPrecio();
    }

    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConfiguracionComponente> componentes = new ArrayList<>();

    public void agregarComponente(ConfiguracionComponente configuracionComponente) { // TODO: Ver cuando llegue el momento
        componentes.add(configuracionComponente);
        configuracionComponente.setConfiguracion(this);
    }

    public void eliminarComponente(ConfiguracionComponente configuracionComponente) { // TODO: Ver cuando llegue el momento
        componentes.remove(configuracionComponente);
        configuracionComponente.setConfiguracion(null);
    }
}

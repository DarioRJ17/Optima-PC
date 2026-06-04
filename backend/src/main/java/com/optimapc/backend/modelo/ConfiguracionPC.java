package com.optimapc.backend.modelo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
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

    private String tipoUsoPrevisto;

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

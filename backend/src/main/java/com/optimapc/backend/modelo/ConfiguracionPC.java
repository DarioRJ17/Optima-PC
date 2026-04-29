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
    private Double indicadorEquilibrio; // TODO: Calcular este valor en base a los componentes

    @Transient
    private Double rendimientoPorEuro; // TODO: Calcular este valor en base a los componentes

    @Transient
    public Double getPrecio() {
        return componentes.stream()
                .filter(c -> c.getComponente() != null)
                .mapToDouble(c -> c.getComponente().getPrecio() * c.getCantidad())
                .sum();
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

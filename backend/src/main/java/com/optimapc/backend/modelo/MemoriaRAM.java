package com.optimapc.backend.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "memorias_ram")
public class MemoriaRAM extends Componente {

    private Integer velocidad;
    private String tipoDDR;
    private Integer numModulos;
    private Integer gbPorModulo;
    private String color;
    private Integer latenciaCAS;
}

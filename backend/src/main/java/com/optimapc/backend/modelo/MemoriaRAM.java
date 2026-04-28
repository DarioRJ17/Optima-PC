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

    private Integer capacidad;
    private Integer frecuencia;
    private String tipoDDR;
}

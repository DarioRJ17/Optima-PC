package com.optimapc.backend.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "procesadores")
public class Procesador extends Componente {

    private String socket;
    private Integer nucleos;
    private Integer hilos;
    private Double frecuenciaBase;
}

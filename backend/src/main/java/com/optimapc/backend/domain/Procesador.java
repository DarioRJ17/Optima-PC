package com.optimapc.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "procesadores")
public class Procesador extends Componente {

    private Integer nucleos;
    private Double frecuenciaBase;
    private Double frecuenciaBoost;
    private String microarquitectura;
    private Integer tdp;
    private String graficaIntegrada;
    private String socket;
}

package com.optimapc.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "placas_base")
public class PlacaBase extends Componente {

    private String socket;
    private String factorForma;
    private Integer memoriaMaxima;
    private Integer ranurasMemoria;
    private String color;
    private String tipoDDR;
}

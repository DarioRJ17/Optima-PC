package com.optimapc.backend.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tarjetas_graficas")
public class TarjetaGrafica extends Componente {

    private String chipset;
    private Integer memoria;
    private Integer frecuenciaBase;
    private Integer frecuenciaBoost;
    private String color;
    private Integer longitud;
}

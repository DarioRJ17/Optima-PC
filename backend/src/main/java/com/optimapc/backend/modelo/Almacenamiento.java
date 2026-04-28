package com.optimapc.backend.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "almacenamientos")
public class Almacenamiento extends Componente {

    private Integer capacidad;
    private String tipoUnidad;
    private Double velocidadLectura;
}

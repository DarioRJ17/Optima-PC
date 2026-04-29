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
    private Double precioPorGB;
    private String tipo;
    private String factorForma;
    private String interfaz;
}

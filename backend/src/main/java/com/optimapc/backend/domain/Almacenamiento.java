package com.optimapc.backend.domain;

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
    private String tipo;
    private String factorForma;
    private String interfaz;
}

package com.optimapc.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cajas")
public class Caja extends Componente {

    private String tipo;
    private String color;
    private String panelLateral;
}

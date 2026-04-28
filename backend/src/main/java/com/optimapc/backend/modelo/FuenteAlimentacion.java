package com.optimapc.backend.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fuentes_alimentacion")
public class FuenteAlimentacion extends Componente {

    private Integer potencia;
    private String certificacion;
}
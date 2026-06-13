package com.optimapc.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "refrigeradores_cpu")
public class RefrigeradorCPU extends Componente {

    private Integer rpmMin;
    private Integer rpmMax;
    private Double nivelRuidoMin;
    private Double nivelRuidoMax;
    private String color;
    private Integer tamano;
}
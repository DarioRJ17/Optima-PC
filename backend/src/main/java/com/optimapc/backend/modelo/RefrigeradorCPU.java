package com.optimapc.backend.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "refrigeradores_cpu")
public class RefrigeradorCPU extends Componente {

    private String rpm;
    private String nivelRuido;
    private String color;
    private String tamano;
}
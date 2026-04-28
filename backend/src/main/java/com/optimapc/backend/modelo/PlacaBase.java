package com.optimapc.backend.modelo;

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
    private String chipset;
    private String formato;
    private String tipoDDR;
}

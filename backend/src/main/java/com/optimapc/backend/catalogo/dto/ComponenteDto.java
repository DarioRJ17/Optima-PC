package com.optimapc.backend.catalogo.dto;

public record ComponenteDto(
        Long id,
        String tipo,
        String nombre,
        String especificacion,
        Double precio,
        Integer cantidad) {
}

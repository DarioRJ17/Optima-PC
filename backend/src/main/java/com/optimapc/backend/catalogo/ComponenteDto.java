package com.optimapc.backend.catalogo;

public record ComponenteDto(
        Long id,
        String tipo,
        String nombre,
        String especificacion,
        Double precio,
        Integer cantidad) {
}

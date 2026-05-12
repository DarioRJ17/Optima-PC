package com.optimapc.backend.montarPC.dto;

import java.util.Map;

public record ComponenteDetalleDto(
        Long id,
        String tipo,
        String nombre,
        String especificacion,
        Double precio,
        Integer cantidad,
        Integer consumoWatts,
        Map<String, Object> detalles) {
}

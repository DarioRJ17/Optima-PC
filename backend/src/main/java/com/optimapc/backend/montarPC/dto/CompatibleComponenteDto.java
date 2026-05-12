package com.optimapc.backend.montarPC.dto;

import java.util.List;

public record CompatibleComponenteDto(
        Long id,
        String tipo,
        String nombre,
        String especificacion,
        Double precio,
        Integer cantidad,
        List<CompatibilityWarningDto> warnings) {
}

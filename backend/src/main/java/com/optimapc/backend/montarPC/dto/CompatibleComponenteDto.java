package com.optimapc.backend.montarPC.dto;

import java.util.List;
import java.util.Map;

public record CompatibleComponenteDto(
        Long id,
        String tipo,
        String nombre,
        String especificacion,
        Double precio,
        Integer cantidad,
        List<CompatibilityWarningDto> warnings,
        Map<String, Object> propiedades) {
}

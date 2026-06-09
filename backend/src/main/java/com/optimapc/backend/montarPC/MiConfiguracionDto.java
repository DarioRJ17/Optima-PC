package com.optimapc.backend.montarPC;

import java.time.LocalDateTime;
import java.util.List;

public record MiConfiguracionDto(
        Long id,
        String nombre,
        Double precio,
        LocalDateTime fechaCreacion,
        List<MiConfiguracionComponenteDto> componentes) {}

package com.optimapc.backend.catalogo.dto;

import java.time.LocalDateTime;

public record FavoritoDto(
        Long id,
        LocalDateTime fechaGuardado,
        PremontadoCatalogoDto premontado) {
}

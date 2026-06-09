package com.optimapc.backend.catalogo;

import java.time.LocalDateTime;

public record FavoritoDto(
        Long id,
        LocalDateTime fechaGuardado,
        PremontadoCatalogoDto premontado) {
}

package com.optimapc.backend.catalogo;

import java.time.LocalDateTime;

public record ValoracionDto(
        Long id,
        String usuarioNombre,
        Integer calificacion,
        String comentario,
        LocalDateTime fecha) {
}

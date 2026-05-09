package com.optimapc.backend.auth.dto;

import java.time.LocalDateTime;

public record AuthResponse(
        String mensaje,
        Long id,
        String nombre,
        String apellidos,
        String email,
        LocalDateTime fechaRegistro,
        String token) {
}

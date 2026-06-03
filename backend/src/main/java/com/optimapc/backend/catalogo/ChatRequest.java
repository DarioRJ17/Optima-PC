package com.optimapc.backend.catalogo;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
        @NotBlank(message = "El mensaje es obligatorio")
        String mensaje,
        List<MensajeHistorial> historial) {

    public record MensajeHistorial(String rol, String contenido) {}
}

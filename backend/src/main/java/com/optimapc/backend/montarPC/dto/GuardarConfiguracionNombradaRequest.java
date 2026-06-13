package com.optimapc.backend.montarPC.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record GuardarConfiguracionNombradaRequest(
        @NotEmpty List<Long> componenteIds,
        @NotBlank @Size(max = 100) String nombre) {}

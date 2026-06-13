package com.optimapc.backend.montarPC.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record GuardarConfiguracionRequest(@NotEmpty List<Long> componenteIds) {}

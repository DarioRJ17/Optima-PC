package com.optimapc.backend.montarPC;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record GuardarConfiguracionRequest(@NotEmpty List<Long> componenteIds) {}

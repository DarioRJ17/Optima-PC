package com.optimapc.backend.reciclaje.dto;

import java.util.List;

public record ReciclajeTipoUsoDto(
        String tipoUso,
        List<ReciclajeConfiguracionDto> configuraciones
) {}

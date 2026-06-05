package com.optimapc.backend.montarPC;

import java.util.List;

public record ReciclajeTipoUsoDto(
        String tipoUso,
        List<ReciclajeConfiguracionDto> configuraciones
) {}

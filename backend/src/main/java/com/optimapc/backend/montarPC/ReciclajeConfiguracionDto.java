package com.optimapc.backend.montarPC;

import java.util.List;

public record ReciclajeConfiguracionDto(
        List<ReciclajeComponenteDto> componentes,
        double scoreRendimiento,   // rendimientoPorEuro normalizado 0-100
        double scoreEquilibrio,    // indicadorEquilibrio 0-100
        double scoreCompuesto,     // 0.6 * rendimiento + 0.4 * equilibrio
        double precioTotal,
        List<ComponenteDesbalanceado> componentesDesbalanceados
) {}

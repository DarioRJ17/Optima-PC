package com.optimapc.backend.pedido;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoDto(
        Long id,
        LocalDateTime fecha,
        Double total,
        List<ItemPedidoDto> items) {
}

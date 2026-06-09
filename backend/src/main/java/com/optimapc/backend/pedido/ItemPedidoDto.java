package com.optimapc.backend.pedido;

public record ItemPedidoDto(
        Long id,
        Long configuracionId,
        String nombreProducto,
        Integer cantidad,
        Double precioUnitario,
        Double subtotal) {
}

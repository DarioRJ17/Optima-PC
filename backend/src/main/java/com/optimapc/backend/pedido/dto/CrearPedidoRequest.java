package com.optimapc.backend.pedido.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CrearPedidoRequest(
        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Valid
        List<ItemPedidoRequest> items) {
}

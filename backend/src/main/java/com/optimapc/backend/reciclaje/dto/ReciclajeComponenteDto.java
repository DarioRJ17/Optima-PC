package com.optimapc.backend.reciclaje.dto;

public record ReciclajeComponenteDto(
        Long id,
        String nombre,
        String categoria,
        double precio,
        int cantidad,
        boolean esFijo   // true si lo ha aportado el usuario, false si lo sugiere el sistema
) {}

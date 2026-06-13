package com.optimapc.backend.catalogo.dto;

import java.util.List;

public record PremontadoCatalogoDto(
        Long id,
        String titulo,
        String descripcion,
        String marca,
        Integer descuento,
        String sistemaOperativo,
        Integer stock,
        List<String> usosPrevistos,
        String imagenUrl,
        Boolean esReacondicionado,
        Double precio,
        Double precioReducido,
        Double valoracionMedia,
        Integer numeroValoraciones,
        Boolean favorita,
        Double rendimientoPorEuro,
        List<ComponenteDto> componentes,
        long numeroCompras) {
}
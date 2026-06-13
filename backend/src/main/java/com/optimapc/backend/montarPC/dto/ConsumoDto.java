package com.optimapc.backend.montarPC.dto;

public record ConsumoDto(
    int consumoEstimadoW,
    int consumoRecomendadoW,
    Integer potenciaPSUW,
    Integer disponibleW,
    boolean suficiente
) {}

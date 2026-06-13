package com.optimapc.backend.recomendacion.dto;

import java.util.List;

import com.optimapc.backend.domain.TipoUso;

public record EncuestaInicialRequest(
        TipoUso usoPrincipal,
        List<TipoUso> usosSecundariosEncuesta,
        Double presupuesto,
        Boolean preferenciaReacondicionado) {
}
package com.optimapc.backend.usuario.dto;

import java.util.List;

import com.optimapc.backend.modelo.TipoUso;

public record EncuestaInicialRequest(
        TipoUso usoPrincipal,
        List<TipoUso> usosSecundariosEncuesta,
        Double presupuesto,
        Boolean preferenciaReacondicionado) {
}
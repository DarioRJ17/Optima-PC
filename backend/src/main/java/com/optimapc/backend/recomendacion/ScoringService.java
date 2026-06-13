package com.optimapc.backend.recomendacion;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Valoracion;

@Service
public class ScoringService {

    private static final double SCORE_MINIMO = 0.0;

    private static final double TOLERANCIA_PRESUPUESTO = 500.0;
    private static final double UMBRAL_PRIORIZAR_REACONDICIONADO = 0.1;
    private static final double BONUS_REACONDICIONADO_PRIORIZADO = 0.15;
    private static final double SCORE_USO_COINCIDENCIA_TOTAL = 1.0;
    private static final double SCORE_USO_COINCIDENCIA_PARCIAL = 0.5;
    private static final double SCORE_USO_SIN_COINCIDENCIA = 0.0;

    public List<Premontado> recomendarPremontados(PerfilUsuario perfil, List<Premontado> premontados) {
        if (perfil == null || premontados == null || premontados.isEmpty()) {
            return List.of();
        }

        if (perfil.getTipoUsoFrecuente() == null && perfil.getUsosSecundarios().isEmpty()) {
            return List.of();
        }

        // Orden por score de recomendación y, a igualdad, desempate por calidad:
        // primero la mejor valoración media y luego la mejor relación rendimiento/€
        Comparator<Premontado> orden = Comparator
                .comparingDouble((Premontado premontado) -> calcularScoreFinal(perfil, premontado))
                .thenComparingDouble(this::valoracionMedia)
                .thenComparingDouble(premontado -> valorSeguro(premontado.getRendimientoPorEuro()))
                .reversed();

        return premontados.stream()
                .filter(premontado -> cumpleFiltroPresupuesto(perfil, premontado))
                .filter(premontado -> cumpleFiltroReacondicionado(perfil, premontado))
                .sorted(orden)
                .toList();
    }

    private double valoracionMedia(Premontado premontado) {
        if (premontado.getValoraciones() == null || premontado.getValoraciones().isEmpty()) {
            return SCORE_MINIMO;
        }

        return premontado.getValoraciones().stream()
                .mapToInt(Valoracion::getPuntuacion)
                .average()
                .orElse(SCORE_MINIMO);
    }

    private boolean cumpleFiltroPresupuesto(PerfilUsuario perfil, Premontado premontado) {
        Double presupuestoEstimado = perfil.getPresupuestoEstimado();
        if (presupuestoEstimado == null || Double.compare(presupuestoEstimado, SCORE_MINIMO) == 0) {
            return true;
        }

        Double precioEfectivo = obtenerPrecioEfectivo(premontado);
        if (precioEfectivo == null) {
            return false;
        }

        double minimo = presupuestoEstimado - TOLERANCIA_PRESUPUESTO;
        double maximo = presupuestoEstimado + TOLERANCIA_PRESUPUESTO;
        return precioEfectivo >= minimo && precioEfectivo <= maximo;
    }

    private boolean cumpleFiltroReacondicionado(PerfilUsuario perfil, Premontado premontado) {
        if (Boolean.FALSE.equals(perfil.getPreferenciaDeReacondicionado())) {
            return !Boolean.TRUE.equals(premontado.getEsReacondicionado());
        }

        return true;
    }

    private double calcularScoreFinal(PerfilUsuario perfil, Premontado premontado) {
        double scoreUso = calcularScoreUso(perfil, premontado);
        double pesoUso = obtenerPesoUso(perfil, premontado);
        double scoreBase = scoreUso * pesoUso;
        double bonusReacondicionado = calcularBonusReacondicionado(perfil, premontado);

        return scoreBase + bonusReacondicionado;
    }

    private double calcularScoreUso(PerfilUsuario perfil, Premontado premontado) {
        boolean priorizaUsoFrecuente = priorizaUsoFrecuente(perfil);
        boolean coincideGrupoPrioritario = priorizaUsoFrecuente
                ? contieneUso(premontado, perfil.getTipoUsoFrecuente())
                : contieneAlgunoDeLosUsos(premontado, perfil.getUsosSecundarios());
        boolean coincideGrupoSecundario = priorizaUsoFrecuente
                ? contieneAlgunoDeLosUsos(premontado, perfil.getUsosSecundarios())
                : contieneUso(premontado, perfil.getTipoUsoFrecuente());

        if (coincideGrupoPrioritario) {
            return SCORE_USO_COINCIDENCIA_TOTAL;
        }

        if (coincideGrupoSecundario) {
            return SCORE_USO_COINCIDENCIA_PARCIAL;
        }

        return SCORE_USO_SIN_COINCIDENCIA;
    }

    private double obtenerPesoUso(PerfilUsuario perfil, Premontado premontado) {
        boolean priorizaUsoFrecuente = priorizaUsoFrecuente(perfil);
        if (priorizaUsoFrecuente) {
            return perfil.getScoreTipoUsoFrecuente() != null ? perfil.getScoreTipoUsoFrecuente() : SCORE_MINIMO;
        }

        return perfil.getScoreUsosSecundarios() != null ? perfil.getScoreUsosSecundarios() : SCORE_MINIMO;
    }

    private boolean priorizaUsoFrecuente(PerfilUsuario perfil) {
        return valorSeguro(perfil.getScoreTipoUsoFrecuente()) >= valorSeguro(perfil.getScoreUsosSecundarios());
    }

    private double calcularBonusReacondicionado(PerfilUsuario perfil, Premontado premontado) {
        if (!Boolean.TRUE.equals(perfil.getPreferenciaDeReacondicionado())) {
            return SCORE_MINIMO;
        }

        if (valorSeguro(perfil.getScorePreferenciaDeReacondicionado()) < UMBRAL_PRIORIZAR_REACONDICIONADO) {
            return SCORE_MINIMO;
        }

        return Boolean.TRUE.equals(premontado.getEsReacondicionado())
                ? BONUS_REACONDICIONADO_PRIORIZADO
                : SCORE_MINIMO;
    }

    private boolean contieneUso(Premontado premontado, TipoUso uso) {
        if (uso == null || premontado.getUsosPrevistos() == null) {
            return false;
        }

        return premontado.getUsosPrevistos().contains(uso);
    }

    private boolean contieneAlgunoDeLosUsos(Premontado premontado, List<TipoUso> usos) {
        if (usos == null || usos.isEmpty() || premontado.getUsosPrevistos() == null) {
            return false;
        }

        return premontado.getUsosPrevistos().stream().anyMatch(usos::contains);
    }

    private Double obtenerPrecioEfectivo(Premontado premontado) {
        Double precioReducido = premontado.getPrecioReducido();
        return precioReducido != null ? precioReducido : premontado.getPrecio();
    }

    private double valorSeguro(Double value) {
        return value != null ? value : SCORE_MINIMO;
    }
}

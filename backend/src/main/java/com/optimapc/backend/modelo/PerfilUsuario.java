package com.optimapc.backend.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.optimapc.backend.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "perfiles_usuario")
public class PerfilUsuario {

    private static final double SCORE_MINIMO = 0.0;
    private static final double SCORE_MAXIMO = 1.0;

    private static final double PESO_ACCION_COMPRA = 0.16;
    private static final double PESO_ACCION_VALORACION = 0.12;
    private static final double PESO_ACCION_FAVORITO = 0.08;

    private static final double COEF_DECAIMIENTO_SCORE = 0.85;
    private static final double COEF_IMPACTO_NUEVO = 0.15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private TipoUso tipoUsoFrecuente;

    @Column(nullable = false)
    private Double scoreTipoUsoFrecuente = 0.0;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @OrderColumn(name = "orden")
    @Column(name = "tipo_uso", nullable = false)
    private List<TipoUso> usosSecundarios = new ArrayList<>(); // se guarda en tabla separada, mantiene el orden de los usos secundarios

    @Column(nullable = false)
    private Double scoreUsosSecundarios = 0.0;

    private Double presupuestoEstimado;

    @Column(nullable = false)
    private Double scorePresupuestoEstimado = 0.0;

    private Boolean preferenciaDeReacondicionado = false;

    @Column(nullable = false)
    private Double scorePreferenciaDeReacondicionado = 0.0;

    private LocalDateTime fechaUltimaActualizacion;

    @PrePersist
    void prePersist() {
        if (presupuestoEstimado == null) {
            presupuestoEstimado = 0.0;
        }
        if (preferenciaDeReacondicionado == null) {
            preferenciaDeReacondicionado = false;
        }
        if (scoreTipoUsoFrecuente == null) {
            scoreTipoUsoFrecuente = SCORE_MINIMO;
        }
        if (scoreUsosSecundarios == null) {
            scoreUsosSecundarios = SCORE_MINIMO;
        }
        if (scorePresupuestoEstimado == null) {
            scorePresupuestoEstimado = SCORE_MINIMO;
        }
        if (scorePreferenciaDeReacondicionado == null) {
            scorePreferenciaDeReacondicionado = SCORE_MINIMO;
        }
        fechaUltimaActualizacion = LocalDateTime.now();
    }

    public void actualizarDesdeEncuesta(TipoUso usoPrincipal, List<TipoUso> usosSecundariosEncuesta, Double presupuesto, Boolean preferenciaReacondicionado) {
        if (usoPrincipal != null) {
            tipoUsoFrecuente = usoPrincipal;
            scoreTipoUsoFrecuente = SCORE_MAXIMO;
        } else {
            scoreTipoUsoFrecuente = SCORE_MINIMO;
        }

        if (usosSecundariosEncuesta != null) {
            this.usosSecundarios.clear();
            for (TipoUso uso : usosSecundariosEncuesta) {
                if (uso == usoPrincipal) {
                    continue;
                }
                agregarUsoSecundario(uso);
            }
            scoreUsosSecundarios = this.usosSecundarios.isEmpty() ? SCORE_MINIMO : SCORE_MAXIMO;
        } else {
            scoreUsosSecundarios = SCORE_MINIMO;
        }

        if (presupuesto != null) {
            presupuestoEstimado = presupuesto;
            scorePresupuestoEstimado = SCORE_MAXIMO;
        } else {
            scorePresupuestoEstimado = SCORE_MINIMO;
        }

        if (preferenciaReacondicionado != null) {
            preferenciaDeReacondicionado = preferenciaReacondicionado;
            scorePreferenciaDeReacondicionado = preferenciaReacondicionado ? SCORE_MAXIMO : SCORE_MINIMO;
        } else {
            scorePreferenciaDeReacondicionado = SCORE_MINIMO;
        }

        fechaUltimaActualizacion = LocalDateTime.now();
    }

    public void actualizarDesdeCompra(TipoUso usoInferido, Double nuevoPresupuesto, Boolean compraReacondicionado) {
        if (nuevoPresupuesto != null) {
            presupuestoEstimado = presupuestoEstimado == null || presupuestoEstimado <= 0.0
                    ? nuevoPresupuesto
                    : (presupuestoEstimado * 0.6) + (nuevoPresupuesto * 0.4);
        }

        if (Boolean.TRUE.equals(compraReacondicionado)) {
            preferenciaDeReacondicionado = true;
        }

        actualizarDesdeUsoInferido(usoInferido);

        aplicarImpactoGeneral(PESO_ACCION_COMPRA);
        aplicarImpactoReacondicionado(Boolean.TRUE.equals(compraReacondicionado), PESO_ACCION_COMPRA);

        fechaUltimaActualizacion = LocalDateTime.now();
    }

    public void actualizarDesdeValoracion(Set<TipoUso> usosInferidos, Double presupuestoRelacionado, Boolean compraReacondicionado, Integer puntuacion) {
        if (presupuestoRelacionado != null) {
            presupuestoEstimado = presupuestoEstimado == null || presupuestoEstimado <= 0.0
                    ? presupuestoRelacionado
                    : (presupuestoEstimado * 0.8) + (presupuestoRelacionado * 0.2);
        }

        if (Boolean.TRUE.equals(compraReacondicionado)) {
            preferenciaDeReacondicionado = true;
        }

        if (usosInferidos != null) {
            usosInferidos.forEach(this::actualizarDesdeUsoInferido);
        }

        double impactoValoracion = PESO_ACCION_VALORACION * calcularIntensidadValoracion(puntuacion);
        aplicarImpactoGeneral(impactoValoracion);
        aplicarImpactoReacondicionado(Boolean.TRUE.equals(compraReacondicionado), impactoValoracion);

        fechaUltimaActualizacion = LocalDateTime.now();
    }

    public void actualizarDesdeFavorito(TipoUso usoInferido, Double presupuestoRelacionado, Boolean compraReacondicionado) {
        if (presupuestoRelacionado != null) {
            presupuestoEstimado = presupuestoEstimado == null || presupuestoEstimado <= 0.0
                    ? presupuestoRelacionado
                    : (presupuestoEstimado * 0.9) + (presupuestoRelacionado * 0.1);
        }

        if (Boolean.TRUE.equals(compraReacondicionado)) {
            preferenciaDeReacondicionado = true;
        }

        actualizarDesdeUsoInferido(usoInferido);

        aplicarImpactoGeneral(PESO_ACCION_FAVORITO);
        aplicarImpactoReacondicionado(Boolean.TRUE.equals(compraReacondicionado), PESO_ACCION_FAVORITO);

        fechaUltimaActualizacion = LocalDateTime.now();
    }

    private void agregarUsoSecundario(TipoUso uso) {
        if (uso == null || usosSecundarios.contains(uso)) {
            return;
        }

        usosSecundarios.add(uso);
    }

    private void actualizarDesdeUsoInferido(TipoUso uso) {
        if (uso == null) {
            return;
        }

        if (tipoUsoFrecuente == null) {
            tipoUsoFrecuente = uso;
        } else if (tipoUsoFrecuente != uso) {
            agregarUsoSecundario(uso);
        }
    }

    private void aplicarImpactoGeneral(double impactoBase) {
        scoreTipoUsoFrecuente = limitarScore((scoreTipoUsoFrecuente * COEF_DECAIMIENTO_SCORE) + (impactoBase * COEF_IMPACTO_NUEVO));
        scoreUsosSecundarios = limitarScore((scoreUsosSecundarios * COEF_DECAIMIENTO_SCORE) + (impactoBase * COEF_IMPACTO_NUEVO));
        scorePresupuestoEstimado = limitarScore((scorePresupuestoEstimado * COEF_DECAIMIENTO_SCORE) + (impactoBase * COEF_IMPACTO_NUEVO));
    }

    private void aplicarImpactoReacondicionado(boolean esReacondicionado, double impactoBase) {
        if (esReacondicionado) {
            scorePreferenciaDeReacondicionado = limitarScore(
                (scorePreferenciaDeReacondicionado * COEF_DECAIMIENTO_SCORE) + (impactoBase * COEF_IMPACTO_NUEVO)
            );
        } else {
            scorePreferenciaDeReacondicionado = limitarScore(
                scorePreferenciaDeReacondicionado * COEF_DECAIMIENTO_SCORE
            );
        }
    }

    private double calcularIntensidadValoracion(Integer puntuacion) {
        if (puntuacion == null) {
            return 0.5;
        }

        return switch (Math.max(1, Math.min(5, puntuacion))) {
            case 1 -> 0.25;
            case 2 -> 0.50;
            case 3 -> 0.75;
            case 4 -> 1.00;
            default -> 1.00; // tope en 1.0
        };
    }

    private double limitarScore(double score) {
        return Math.max(SCORE_MINIMO, Math.min(SCORE_MAXIMO, score));
    }
}
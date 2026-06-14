package com.optimapc.backend.recomendacion;

import static com.optimapc.backend.support.TestData.premontado;
import static com.optimapc.backend.support.TestData.valoraciones;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoUso;

class ScoringServiceTest {

    private ScoringService service;

    @BeforeEach
    void setUp() {
        service = new ScoringService();
    }

    private PerfilUsuario perfilGaming() {
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setTipoUsoFrecuente(TipoUso.GAMING);
        perfil.setScoreTipoUsoFrecuente(1.0);
        perfil.setScoreUsosSecundarios(0.0);
        perfil.setPreferenciaDeReacondicionado(false); // valor real por defecto del dominio
        return perfil;
    }

    @Test
    void perfilNuloDevuelveListaVacia() {
        assertThat(service.recomendarPremontados(null, List.of(premontado(1L, 1000.0, false, TipoUso.GAMING)))).isEmpty();
    }

    @Test
    void listaNulaOVaciaDevuelveListaVacia() {
        assertThat(service.recomendarPremontados(perfilGaming(), null)).isEmpty();
        assertThat(service.recomendarPremontados(perfilGaming(), List.of())).isEmpty();
    }

    @Test
    void perfilSinUsosDevuelveListaVacia() {
        PerfilUsuario perfilVacio = new PerfilUsuario(); // sin tipoUsoFrecuente ni usos secundarios
        assertThat(service.recomendarPremontados(perfilVacio, List.of(premontado(1L, 1000.0, false, TipoUso.GAMING)))).isEmpty();
    }

    @Test
    void premontadoQueCoincideConUsoVaPrimero() {
        Premontado gaming = premontado(1L, 1000.0, false, TipoUso.GAMING);
        Premontado ofimatica = premontado(2L, 1000.0, false, TipoUso.OFIMATICA);

        List<Premontado> resultado = service.recomendarPremontados(perfilGaming(), List.of(ofimatica, gaming));

        assertThat(resultado).containsExactly(gaming, ofimatica);
    }

    @Test
    void filtraPremontadosFueraDelPresupuestoConTolerancia() {
        PerfilUsuario perfil = perfilGaming();
        perfil.setPresupuestoEstimado(1000.0); // ventana [500, 1500]

        Premontado dentro = premontado(1L, 1200.0, false, TipoUso.GAMING);
        Premontado fuera = premontado(2L, 2000.0, false, TipoUso.GAMING);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(dentro, fuera));

        assertThat(resultado).containsExactly(dentro);
    }

    @Test
    void presupuestoCeroNoFiltra() {
        PerfilUsuario perfil = perfilGaming();
        perfil.setPresupuestoEstimado(0.0);
        Premontado caro = premontado(1L, 5000.0, false, TipoUso.GAMING);
        assertThat(service.recomendarPremontados(perfil, List.of(caro))).containsExactly(caro);
    }

    @Test
    void perfilSinReacondicionadoExcluyeReacondicionados() {
        PerfilUsuario perfil = perfilGaming(); // preferencia ya es false

        Premontado nuevo = premontado(1L, 1000.0, false, TipoUso.GAMING);
        Premontado reacondicionado = premontado(2L, 1000.0, true, TipoUso.GAMING);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(nuevo, reacondicionado));

        assertThat(resultado).containsExactly(nuevo);
    }

    @Test
    void preferenciaReacondicionadoAplicaBonus() {
        PerfilUsuario perfil = perfilGaming();
        perfil.setPreferenciaDeReacondicionado(true);
        perfil.setScorePreferenciaDeReacondicionado(0.5); // >= umbral 0.1

        Premontado nuevo = premontado(1L, 1000.0, false, TipoUso.GAMING);
        Premontado reacondicionado = premontado(2L, 1000.0, true, TipoUso.GAMING);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(nuevo, reacondicionado));

        assertThat(resultado).containsExactly(reacondicionado, nuevo);
    }

    @Test
    void desempataPorMejorValoracionMedia() {
        Premontado mejorValorado = premontado(1L, 1000.0, false, TipoUso.GAMING);
        mejorValorado.getValoraciones().addAll(valoraciones(5, 5));
        Premontado peorValorado = premontado(2L, 1000.0, false, TipoUso.GAMING);
        peorValorado.getValoraciones().addAll(valoraciones(2));

        List<Premontado> resultado = service.recomendarPremontados(perfilGaming(), List.of(peorValorado, mejorValorado));

        assertThat(resultado).containsExactly(mejorValorado, peorValorado);
    }
}

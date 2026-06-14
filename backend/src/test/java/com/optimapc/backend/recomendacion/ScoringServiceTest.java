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

    @Test
    void perfilSoloConUsosSecundariosRecomienda() {
        // tipoUsoFrecuente == null pero hay usos secundarios -> debe recomendar (no lista vacía)
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setTipoUsoFrecuente(null);
        perfil.setScoreTipoUsoFrecuente(null); // fuerza el valor por defecto en valorSeguro
        perfil.setUsosSecundarios(List.of(TipoUso.GAMING));
        perfil.setScoreUsosSecundarios(1.0);

        Premontado gaming = premontado(1L, 1000.0, false, TipoUso.GAMING);
        Premontado ofimatica = premontado(2L, 1000.0, false, TipoUso.OFIMATICA);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(ofimatica, gaming));

        assertThat(resultado).containsExactly(gaming, ofimatica);
    }

    @Test
    void coincidenciaSoloConUsoSecundarioPuntuaParcial() {
        // El uso frecuente prioriza (score 1.0), pero el premontado solo coincide con el secundario
        PerfilUsuario perfil = perfilGaming();
        perfil.setUsosSecundarios(List.of(TipoUso.EDICION));

        Premontado gaming = premontado(1L, 1000.0, false, TipoUso.GAMING);
        Premontado soloEdicion = premontado(2L, 1000.0, false, TipoUso.EDICION);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(soloEdicion, gaming));

        // gaming (coincidencia total) por delante de edicion (coincidencia parcial por uso secundario)
        assertThat(resultado).containsExactly(gaming, soloEdicion);
    }

    @Test
    void presupuestoPorDebajoDeLaVentanaFiltra() {
        PerfilUsuario perfil = perfilGaming();
        perfil.setPresupuestoEstimado(1000.0); // ventana [500, 1500]

        Premontado dentro = premontado(1L, 1000.0, false, TipoUso.GAMING);
        Premontado demasiadoBarato = premontado(2L, 100.0, false, TipoUso.GAMING);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(dentro, demasiadoBarato));

        assertThat(resultado).containsExactly(dentro);
    }

    @Test
    void aplicaElPrecioConDescuentoAlFiltrarPorPresupuesto() {
        PerfilUsuario perfil = perfilGaming();
        perfil.setPresupuestoEstimado(500.0); // ventana [0, 1000]

        // Precio base 1100 (fuera de ventana) pero con 50% de descuento -> 550 (dentro)
        Premontado conDescuento = premontado(1L, 1100.0, false, TipoUso.GAMING);
        conDescuento.setDescuento(50);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(conDescuento));

        assertThat(resultado).containsExactly(conDescuento);
    }

    @Test
    void preferenciaReacondicionadoConScoreBajoNoAplicaBonus() {
        PerfilUsuario perfil = perfilGaming();
        perfil.setPreferenciaDeReacondicionado(true);
        perfil.setScorePreferenciaDeReacondicionado(0.05); // < umbral 0.1 -> sin bonus

        Premontado nuevoMejorValorado = premontado(1L, 1000.0, false, TipoUso.GAMING);
        nuevoMejorValorado.getValoraciones().addAll(valoraciones(5, 5));
        Premontado reacondicionado = premontado(2L, 1000.0, true, TipoUso.GAMING);
        reacondicionado.getValoraciones().addAll(valoraciones(1));

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(reacondicionado, nuevoMejorValorado));

        // Sin bonus, el nuevo (mejor valorado) gana al reacondicionado
        assertThat(resultado).containsExactly(nuevoMejorValorado, reacondicionado);
    }

    @Test
    void scoresNulosUsanValorPorDefectoSinRomper() {
        // Ambos scores null -> prioriza uso frecuente y el peso cae al valor por defecto (0.0)
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setTipoUsoFrecuente(TipoUso.GAMING);
        perfil.setScoreTipoUsoFrecuente(null);
        perfil.setScoreUsosSecundarios(null);

        Premontado gaming = premontado(1L, 1000.0, false, TipoUso.GAMING);

        List<Premontado> resultado = service.recomendarPremontados(perfil, List.of(gaming));

        assertThat(resultado).containsExactly(gaming);
    }
}

package com.optimapc.backend.recomendacion;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.optimapc.backend.catalogo.PremontadoCatalogoService;
import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.domain.Valoracion;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.recomendacion.dto.EncuestaInicialRequest;

/**
 * Test de INTEGRACIÓN de la SALIDA del recomendador: complementa a las 4 entradas.
 *
 * Reproduce el flujo real del CatalogoController:
 *   premontadoCatalogoService.obtenerTodosLosPremontados()  (catalogo, lee de BD)
 *   → scoringService.recomendarPremontados(perfil, premontados)  (recomendacion)
 *
 * Colaboran las clases REALES de catalogo + recomendacion sobre una BD real (H2): el scoring
 * consume premontados con sus valoraciones cargadas desde la BD y el perfil persistido. Verifica
 * que un perfil produce el ORDEN correcto (coincidencia de uso, desempate por valoración) y que
 * el filtro de reacondicionado se aplica de extremo a extremo.
 */
@DataJpaTest
@Import({RendimientoService.class, PerfilUsuarioService.class, PremontadoCatalogoService.class,
        ScoringService.class})
class RecomendacionSalidaIntegrationTest {

    @Autowired private PerfilUsuarioService perfilUsuarioService;
    @Autowired private PremontadoCatalogoService premontadoCatalogoService;
    @Autowired private ScoringService scoringService;
    @Autowired private PerfilUsuarioRepository perfilUsuarioRepository;
    @Autowired private TestEntityManager em;

    @Test
    void unPerfilGamingPriorizaPremontadosGamingYDesempataPorValoracion() {
        Long usuarioId = persistUsuarioConPerfil(TipoUso.GAMING, 1000.0);
        Usuario evaluador = persistUsuario("rater@test.com");

        Premontado gamingMejorValorado = persistPremontado(1000.0, false, TipoUso.GAMING);
        Premontado gamingSinValorar = persistPremontado(1000.0, false, TipoUso.GAMING);
        Premontado ofimatica = persistPremontado(1000.0, false, TipoUso.OFIMATICA);
        persistValoracion(gamingMejorValorado, evaluador, 5);

        List<Premontado> recomendados = recomendarPara(usuarioId);

        // Ambos gaming por delante del de ofimática (que no coincide en uso, score 0)...
        // ...y entre los dos gaming (mismo score de uso), primero el de mejor valoración media.
        assertThat(recomendados)
                .extracting(Premontado::getId)
                .containsExactly(
                        gamingMejorValorado.getId(),
                        gamingSinValorar.getId(),
                        ofimatica.getId());
    }

    @Test
    void siElPerfilNoQuiereReacondicionadoSeExcluyenDelResultado() {
        Long usuarioId = persistUsuarioConPerfil(TipoUso.GAMING, 1000.0);

        Premontado nuevo = persistPremontado(1000.0, false, TipoUso.GAMING);
        Premontado reacondicionado = persistPremontado(1000.0, true, TipoUso.GAMING);

        List<Premontado> recomendados = recomendarPara(usuarioId);

        assertThat(recomendados).extracting(Premontado::getId)
                .contains(nuevo.getId())
                .doesNotContain(reacondicionado.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Reproduce el flujo del controller: catálogo (con colecciones inicializadas) → scoring. */
    private List<Premontado> recomendarPara(Long usuarioId) {
        em.flush();
        em.clear();
        PerfilUsuario perfil = perfilUsuarioRepository.findByUsuario_Id(usuarioId).orElseThrow();
        List<Premontado> premontados = premontadoCatalogoService.obtenerTodosLosPremontados();
        return scoringService.recomendarPremontados(perfil, premontados);
    }

    private <T extends Componente> T persist(T c) {
        c.setId(null);
        return em.persistAndFlush(c);
    }

    private Usuario persistUsuario(String email) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return em.persistAndFlush(u);
    }

    private Long persistUsuarioConPerfil(TipoUso usoPrincipal, double presupuesto) {
        Usuario usuario = persistUsuario("ana@test.com");
        perfilUsuarioService.guardarEncuestaInicial(usuario.getId(),
                new EncuestaInicialRequest(usoPrincipal, List.of(), presupuesto, false));
        return usuario.getId();
    }

    private Premontado persistPremontado(double precio, boolean reacondicionado, TipoUso... usos) {
        Almacenamiento comp = persist(almacenamiento(null, "SSD", "M.2 NVMe", 1000, precio));
        Premontado p = new Premontado();
        p.setNombre("Equipo");
        p.setMarca("OptimaPC");
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(5);
        p.setEsReacondicionado(reacondicionado);
        ConfiguracionComponente cc = new ConfiguracionComponente();
        cc.setComponente(comp);
        cc.setCategoria("STORAGE");
        cc.setCantidad(1);
        p.agregarComponente(cc);
        p.getUsosPrevistos().addAll(Arrays.asList(usos));
        return em.persistAndFlush(p);
    }

    private void persistValoracion(Premontado premontado, Usuario usuario, int puntuacion) {
        Valoracion v = new Valoracion();
        v.setPremontado(premontado);
        v.setUsuario(usuario);
        v.setPuntuacion(puntuacion);
        v.setFecha(LocalDateTime.now());
        em.persistAndFlush(v);
    }
}

package com.optimapc.backend.recomendacion;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.optimapc.backend.catalogo.FavoritoService;
import com.optimapc.backend.catalogo.PremontadoCatalogoService;
import com.optimapc.backend.catalogo.dto.ValoracionRequest;
import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.pedido.PedidoService;
import com.optimapc.backend.pedido.dto.CrearPedidoRequest;
import com.optimapc.backend.pedido.dto.ItemPedidoRequest;
import com.optimapc.backend.recomendacion.dto.EncuestaInicialRequest;

/**
 * Tests de INTEGRACIÓN organizados por las 4 ENTRADAS que retroalimentan el sistema de
 * recomendación. Cada acción del usuario actualiza el PerfilUsuario (la entrada del recomendador),
 * y cada una atraviesa varios módulos colaborando con sus clases REALES sobre una BD real (H2):
 *
 *   1. Encuesta inicial → recomendacion.PerfilUsuarioService          (recomendacion + usuario)
 *   2. Favorito         → catalogo.FavoritoService                    (catalogo + recomendacion + usuario)
 *   3. Compra           → pedido.PedidoService                        (pedido + recomendacion + catalogo + usuario)
 *   4. Valoración       → catalogo.PremontadoCatalogoService          (catalogo + recomendacion + usuario)
 *
 * @DataJpaTest aporta repositorios + H2 + TestEntityManager; @Import registra los @Service para
 * que se inyecten entre sí como en producción. Tras cada acción hacemos flush+clear para releer
 * el perfil desde la BD y comprobar que la señal se ha persistido de extremo a extremo.
 */
@DataJpaTest
@Import({RendimientoService.class, PerfilUsuarioService.class, FavoritoService.class,
        PedidoService.class, PremontadoCatalogoService.class})
class RecomendacionEntradasIntegrationTest {

    @Autowired private PerfilUsuarioService perfilUsuarioService;
    @Autowired private FavoritoService favoritoService;
    @Autowired private PedidoService pedidoService;
    @Autowired private PremontadoCatalogoService premontadoCatalogoService;
    @Autowired private PerfilUsuarioRepository perfilUsuarioRepository;
    @Autowired private TestEntityManager em;

    // -------------------------------------------------------------------------
    // 1. ENCUESTA INICIAL → crea el perfil base
    // -------------------------------------------------------------------------
    @Test
    void laEncuestaInicialCreaElPerfilBaseDelRecomendador() {
        Usuario usuario = persistUsuario();

        perfilUsuarioService.guardarEncuestaInicial(usuario.getId(),
                new EncuestaInicialRequest(TipoUso.GAMING, List.of(TipoUso.EDICION), 1200.0, false));

        PerfilUsuario perfil = recargarPerfil(usuario.getId());
        assertThat(perfil.getTipoUsoFrecuente()).isEqualTo(TipoUso.GAMING);
        assertThat(perfil.getScoreTipoUsoFrecuente()).isEqualTo(1.0);
        assertThat(perfil.getPresupuestoEstimado()).isEqualTo(1200.0);
        assertThat(perfil.getScorePresupuestoEstimado()).isEqualTo(1.0);
        assertThat(perfil.getUsosSecundarios()).containsExactly(TipoUso.EDICION);
    }

    // -------------------------------------------------------------------------
    // 2. FAVORITO → ajusta el perfil (peso bajo: presupuesto 0.9/0.1)
    // -------------------------------------------------------------------------
    @Test
    void marcarFavoritoAjustaElPerfilSegunElPremontado() {
        Long usuarioId = persistUsuarioConPerfil(TipoUso.OFIMATICA, 800.0);
        Premontado premontado = persistPremontado(1000.0, false, TipoUso.GAMING);

        favoritoService.añadir(usuarioId, premontado.getId());

        PerfilUsuario perfil = recargarPerfil(usuarioId);
        // presupuesto = 800*0.9 + 1000*0.1 = 820
        assertThat(perfil.getPresupuestoEstimado()).isCloseTo(820.0, within(0.01));
        // el uso del premontado (GAMING), distinto al frecuente (OFIMATICA), entra como secundario
        assertThat(perfil.getUsosSecundarios()).contains(TipoUso.GAMING);
    }

    // -------------------------------------------------------------------------
    // 3. COMPRA → ajusta el perfil (peso alto: presupuesto 0.6/0.4)
    // -------------------------------------------------------------------------
    @Test
    void comprarUnPremontadoActualizaElPerfilConMasPeso() {
        Long usuarioId = persistUsuarioConPerfil(TipoUso.OFIMATICA, 800.0);
        Premontado premontado = persistPremontado(1000.0, false, TipoUso.GAMING);

        pedidoService.crear(usuarioId, new CrearPedidoRequest(
                List.of(new ItemPedidoRequest(premontado.getId(), 1))));

        PerfilUsuario perfil = recargarPerfil(usuarioId);
        // presupuesto = 800*0.6 + 1000*0.4 = 880 (más peso que el favorito)
        assertThat(perfil.getPresupuestoEstimado()).isCloseTo(880.0, within(0.01));
        assertThat(perfil.getUsosSecundarios()).contains(TipoUso.GAMING);
    }

    // -------------------------------------------------------------------------
    // 4. VALORACIÓN → ajusta el perfil (presupuesto 0.8/0.2, intensidad por puntuación)
    // -------------------------------------------------------------------------
    @Test
    void valorarUnPremontadoRetroalimentaElPerfil() {
        Long usuarioId = persistUsuarioConPerfil(TipoUso.OFIMATICA, 800.0);
        Premontado premontado = persistPremontado(1000.0, false, TipoUso.GAMING);

        premontadoCatalogoService.crearValoracion(premontado.getId(), usuarioId,
                new ValoracionRequest(5, "Muy contento"));

        PerfilUsuario perfil = recargarPerfil(usuarioId);
        // presupuesto = 800*0.8 + 1000*0.2 = 840
        assertThat(perfil.getPresupuestoEstimado()).isCloseTo(840.0, within(0.01));
        assertThat(perfil.getUsosSecundarios()).contains(TipoUso.GAMING);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private <T extends Componente> T persist(T c) {
        c.setId(null);
        return em.persistAndFlush(c);
    }

    private Usuario persistUsuario() {
        Usuario u = new Usuario();
        u.setEmail("ana@test.com");
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return em.persistAndFlush(u);
    }

    /** Usuario con un perfil ya sembrado por la encuesta inicial (precondición de favorito/compra/valoración). */
    private Long persistUsuarioConPerfil(TipoUso usoPrincipal, double presupuesto) {
        Usuario usuario = persistUsuario();
        perfilUsuarioService.guardarEncuestaInicial(usuario.getId(),
                new EncuestaInicialRequest(usoPrincipal, List.of(), presupuesto, false));
        return usuario.getId();
    }

    /** Premontado persistido con un componente real para que tenga precio efectivo conocido. */
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

    private PerfilUsuario recargarPerfil(Long usuarioId) {
        em.flush();
        em.clear();
        return perfilUsuarioRepository.findByUsuario_Id(usuarioId).orElseThrow();
    }
}

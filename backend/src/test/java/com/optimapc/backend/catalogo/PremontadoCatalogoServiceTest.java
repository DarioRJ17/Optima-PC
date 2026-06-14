package com.optimapc.backend.catalogo;

import static com.optimapc.backend.support.TestData.premontadoCompleto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.catalogo.dto.PremontadoCatalogoDto;
import com.optimapc.backend.catalogo.dto.ValoracionDto;
import com.optimapc.backend.catalogo.dto.ValoracionRequest;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.domain.Valoracion;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.pedido.ItemPedidoRepository;
import com.optimapc.backend.recomendacion.PerfilUsuarioRepository;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PremontadoCatalogoServiceTest {

    @Mock
    private PremontadoRepository premontadoRepository;
    @Mock
    private ValoracionRepository valoracionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    private PremontadoCatalogoService service;

    @BeforeEach
    void setUp() {
        service = new PremontadoCatalogoService(premontadoRepository, valoracionRepository,
                usuarioRepository, new RendimientoService(), perfilUsuarioRepository, itemPedidoRepository);
    }

    private Premontado gamingAsus() {
        return premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING);
    }

    private Premontado ofimaticaHp() {
        return premontadoCompleto(2L, "HP", "Office", null, true, TipoUso.OFIMATICA);
    }

    @Test
    void listarMapeaYNormalizaTodosLosPremontados() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus(), ofimaticaHp()));
        when(itemPedidoRepository.sumComprasPorPremontado()).thenReturn(List.<Object[]>of(new Object[]{1L, 4L}));

        List<PremontadoCatalogoDto> dtos = service.listar();

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(PremontadoCatalogoDto::marca).contains("Asus", "HP");
        PremontadoCatalogoDto asus = dtos.stream().filter(d -> d.id().equals(1L)).findFirst().orElseThrow();
        assertThat(asus.numeroCompras()).isEqualTo(4L);
        assertThat(asus.precioReducido()).isNotNull(); // tiene descuento
        assertThat(asus.componentes()).hasSize(4);
    }

    @Test
    void listarAplicaFiltrosDePrecioMarcaUsoYReacondicionado() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus(), ofimaticaHp()));
        when(itemPedidoRepository.sumComprasPorPremontado()).thenReturn(List.of());

        // Solo gaming, marca Asus, no reacondicionado, precio holgado
        List<PremontadoCatalogoDto> dtos = service.listar(0.0, 5000.0, List.of(TipoUso.GAMING), "asus", false);

        assertThat(dtos).extracting(PremontadoCatalogoDto::marca).containsExactly("Asus");
    }

    @Test
    void obtenerTodosInicializaColecciones() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus()));
        assertThat(service.obtenerTodosLosPremontados()).hasSize(1);
    }

    @Test
    void obtenerPorIdDevuelveDtoSiExiste() {
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(gamingAsus()));
        Optional<PremontadoCatalogoDto> dto = service.obtenerPorId(1L);
        assertThat(dto).isPresent();
        assertThat(dto.get().titulo()).contains("Asus");
    }

    @Test
    void valoracionesDeProductoInexistenteDevuelveListaVacia() {
        when(premontadoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.obtenerValoracionesDelProducto(99L)).isEmpty();
    }

    @Test
    void valoracionesSeDevuelvenOrdenadasPorFecha() {
        Premontado premontado = gamingAsus();
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        premontado.getValoraciones().add(valoracion(premontado, usuario, 4, LocalDateTime.now().minusDays(1)));
        premontado.getValoraciones().add(valoracion(premontado, usuario, 5, LocalDateTime.now()));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(premontado));

        List<ValoracionDto> valoraciones = service.obtenerValoracionesDelProducto(1L);

        assertThat(valoraciones).hasSize(2);
        assertThat(valoraciones.get(0).calificacion()).isEqualTo(5); // la más reciente primero
        assertThat(valoraciones.get(0).usuarioNombre()).isEqualTo("Ana García");
    }

    @Test
    void crearValoracionSinUsuarioLanzaUnauthorized() {
        ValoracionRequest request = new ValoracionRequest(5, "Genial");
        assertThatThrownBy(() -> service.crearValoracion(1L, null, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no autenticado");
    }

    @Test
    void crearValoracionConUsuarioInexistenteLanzaNotFound() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.crearValoracion(1L, 7L, new ValoracionRequest(5, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void crearValoracionDuplicadaLanzaConflict() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(gamingAsus()));
        when(valoracionRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.crearValoracion(1L, 7L, new ValoracionRequest(5, "x")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ya has valorado");
    }

    @Test
    void crearValoracionCorrectaGuardaYActualizaPerfil() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        Premontado premontado = gamingAsus();
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setUsuario(usuario);

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(premontado));
        when(valoracionRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        when(valoracionRepository.save(any(Valoracion.class))).thenAnswer(inv -> {
            Valoracion v = inv.getArgument(0);
            v.setId(500L);
            return v;
        });
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(perfil));

        ValoracionDto dto = service.crearValoracion(1L, 7L, new ValoracionRequest(5, "  Excelente  "));

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.calificacion()).isEqualTo(5);
        assertThat(dto.comentario()).isEqualTo("Excelente"); // normalizado (trim)
        verify(perfilUsuarioRepository).save(perfil);
    }

    @Test
    void normalizarListaPreparaRendimientoPorEuro() {
        Premontado p = gamingAsus();
        service.normalizarLista(List.of(p));
        assertThat(p.getRendimientoPorEuro()).isNotNull();
    }

    private Valoracion valoracion(Premontado premontado, Usuario usuario, int puntuacion, LocalDateTime fecha) {
        Valoracion v = new Valoracion();
        v.setPuntuacion(puntuacion);
        v.setUsuario(usuario);
        v.setPremontado(premontado);
        v.setFecha(fecha);
        return v;
    }
}

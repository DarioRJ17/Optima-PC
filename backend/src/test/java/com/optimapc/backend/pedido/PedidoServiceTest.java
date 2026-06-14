package com.optimapc.backend.pedido;

import static com.optimapc.backend.support.TestData.config;
import static com.optimapc.backend.support.TestData.premontadoCompleto;
import static com.optimapc.backend.support.TestData.procesador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.Pedido;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.pedido.dto.CrearPedidoRequest;
import com.optimapc.backend.pedido.dto.ItemPedidoRequest;
import com.optimapc.backend.pedido.dto.PedidoDto;
import com.optimapc.backend.recomendacion.PerfilUsuarioRepository;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ConfiguracionPCRepository configuracionPCRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilUsuarioRepository perfilUsuarioRepository;

    private PedidoService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new PedidoService(pedidoRepository, configuracionPCRepository, usuarioRepository, perfilUsuarioRepository);
        usuario = new Usuario();
        usuario.setId(7L);
    }

    @Test
    void crearConUsuarioInexistenteLanzaNotFound() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(new ItemPedidoRequest(1L, 1)));
        assertThatThrownBy(() -> service.crear(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void crearConConfiguracionInexistenteLanzaNotFound() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(configuracionPCRepository.findById(99L)).thenReturn(Optional.empty());
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(new ItemPedidoRequest(99L, 1)));
        assertThatThrownBy(() -> service.crear(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Configuración no encontrada");
    }

    @Test
    void crearPedidoConPremontadoCalculaTotalYActualizaPerfil() {
        Premontado premontado = premontadoCompleto(1L, "Asus", "ROG", null, false, TipoUso.GAMING);
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setUsuario(usuario);

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(configuracionPCRepository.findById(1L)).thenReturn(Optional.of(premontado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(80L);
            return p;
        });
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(perfil));

        CrearPedidoRequest request = new CrearPedidoRequest(List.of(new ItemPedidoRequest(1L, 2)));
        PedidoDto dto = service.crear(7L, request);

        assertThat(dto.id()).isEqualTo(80L);
        assertThat(dto.total()).isEqualTo(1170.0 * 2);
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().get(0).nombreProducto()).isEqualTo("Asus ROG");
        verify(perfilUsuarioRepository).save(perfil);
    }

    @Test
    void crearPedidoConConfiguracionPersonalizadaUsaSuNombre() {
        ConfiguracionPC personalizada = config(procesador(1L, "AM5", 8, 5.0, 105, 300.0));
        personalizada.setId(5L);
        personalizada.setNombreConfiguracion("Mi build");

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(configuracionPCRepository.findById(5L)).thenReturn(Optional.of(personalizada));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.empty());

        PedidoDto dto = service.crear(7L, new CrearPedidoRequest(List.of(new ItemPedidoRequest(5L, 1))));

        assertThat(dto.items().get(0).nombreProducto()).isEqualTo("Mi build");
    }

    @Test
    void listarDeUsuarioMapeaPedidos() {
        Pedido pedido = new Pedido();
        pedido.setId(80L);
        pedido.setTotal(1170.0);
        when(pedidoRepository.findAllByUsuario_IdOrderByFechaDesc(7L)).thenReturn(List.of(pedido));
        assertThat(service.listarDeUsuario(7L)).hasSize(1);
    }

    @Test
    void obtenerPorIdExistente() {
        Pedido pedido = new Pedido();
        pedido.setId(80L);
        pedido.setTotal(1170.0);
        when(pedidoRepository.findByIdAndUsuario_Id(80L, 7L)).thenReturn(Optional.of(pedido));
        assertThat(service.obtenerPorId(7L, 80L).id()).isEqualTo(80L);
    }

    @Test
    void obtenerPorIdInexistenteLanzaNotFound() {
        when(pedidoRepository.findByIdAndUsuario_Id(80L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.obtenerPorId(7L, 80L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pedido no encontrado");
    }
}

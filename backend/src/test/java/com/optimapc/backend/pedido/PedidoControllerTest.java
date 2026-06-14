package com.optimapc.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.optimapc.backend.pedido.dto.CrearPedidoRequest;
import com.optimapc.backend.pedido.dto.ItemPedidoRequest;
import com.optimapc.backend.pedido.dto.PedidoDto;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService pedidoService;
    @Mock
    private Authentication authentication;

    private PedidoController controller;

    @BeforeEach
    void setUp() {
        controller = new PedidoController(pedidoService);
        when(authentication.getPrincipal()).thenReturn(7L);
    }

    @Test
    void crearDevuelve201() {
        CrearPedidoRequest request = new CrearPedidoRequest(List.of(new ItemPedidoRequest(1L, 1)));
        PedidoDto dto = new PedidoDto(80L, null, 1170.0, List.of());
        when(pedidoService.crear(7L, request)).thenReturn(dto);

        var http = controller.crear(authentication, request);

        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(http.getBody()).isEqualTo(dto);
    }

    @Test
    void listarDelegaEnElServicio() {
        when(pedidoService.listarDeUsuario(7L)).thenReturn(List.of());
        assertThat(controller.listar(authentication).getBody()).isEmpty();
    }

    @Test
    void obtenerDelegaEnElServicio() {
        PedidoDto dto = new PedidoDto(80L, null, 1170.0, List.of());
        when(pedidoService.obtenerPorId(7L, 80L)).thenReturn(dto);
        assertThat(controller.obtener(authentication, 80L).getBody()).isEqualTo(dto);
    }
}

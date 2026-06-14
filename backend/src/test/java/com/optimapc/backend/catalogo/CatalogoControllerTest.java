package com.optimapc.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.optimapc.backend.catalogo.dto.PremontadoCatalogoDto;
import com.optimapc.backend.catalogo.dto.ValoracionDto;
import com.optimapc.backend.catalogo.dto.ValoracionRequest;
import com.optimapc.backend.chatbot.ChatService;
import com.optimapc.backend.chatbot.dto.ChatRequest;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.recomendacion.PerfilUsuarioService;
import com.optimapc.backend.recomendacion.ScoringService;
import com.optimapc.backend.support.TestData;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    @Mock
    private PremontadoCatalogoService premontadoCatalogoService;
    @Mock
    private PerfilUsuarioService perfilUsuarioService;
    @Mock
    private ScoringService scoringService;
    @Mock
    private ChatService chatService;
    @Mock
    private RendimientoService rendimientoService;
    @Mock
    private Authentication authentication;

    private CatalogoController controller;

    @BeforeEach
    void setUp() {
        controller = new CatalogoController(premontadoCatalogoService, perfilUsuarioService,
                scoringService, chatService, rendimientoService);
    }

    @Test
    void listarConvierteTiposIgnorandoLosInvalidos() {
        when(premontadoCatalogoService.listar(any(), any(), any(), any(), any())).thenReturn(List.of());
        assertThat(controller.listarPremontados(0.0, 2000.0, Set.of("gaming", "noexiste"), "Asus", false)).isEmpty();
    }

    @Test
    void recomendacionesConPerfilDevuelveListaPersonalizada() {
        when(authentication.getPrincipal()).thenReturn(7L);
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setTipoUsoFrecuente(TipoUso.GAMING);
        Premontado premontado = TestData.premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING);
        when(perfilUsuarioService.obtenerPorUsuarioId(7L)).thenReturn(Optional.of(perfil));
        when(premontadoCatalogoService.obtenerTodosLosPremontados()).thenReturn(List.of(premontado));
        when(scoringService.recomendarPremontados(eq(perfil), any())).thenReturn(List.of(premontado));
        when(premontadoCatalogoService.toDto(premontado)).thenReturn(dto());

        var http = controller.obtenerRecomendaciones(authentication);

        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getBody()).hasSize(1);
    }

    @Test
    void recomendacionesSinPerfilDevuelveListaVacia() {
        when(authentication.getPrincipal()).thenReturn(7L);
        when(perfilUsuarioService.obtenerPorUsuarioId(7L)).thenReturn(Optional.empty());
        assertThat(controller.obtenerRecomendaciones(authentication).getBody()).isEmpty();
    }

    @Test
    void obtenerValoracionesDelega() {
        when(premontadoCatalogoService.obtenerValoracionesDelProducto(1L)).thenReturn(List.of());
        assertThat(controller.obtenerValoraciones(1L)).isEmpty();
    }

    @Test
    void crearValoracionDevuelve201() {
        when(authentication.getPrincipal()).thenReturn(7L);
        ValoracionDto valoracion = new ValoracionDto(1L, "Ana", 5, "Bien", null);
        ValoracionRequest request = new ValoracionRequest(5, "Bien");
        when(premontadoCatalogoService.crearValoracion(1L, 7L, request)).thenReturn(valoracion);

        var http = controller.crearValoracion(authentication, 1L, request);

        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(http.getBody()).isEqualTo(valoracion);
    }

    @Test
    void obtenerPremontadoExistenteDevuelve200() {
        when(premontadoCatalogoService.obtenerPorId(1L)).thenReturn(Optional.of(dto()));
        assertThat(controller.obtenerPremontado(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtenerPremontadoInexistenteDevuelve404() {
        when(premontadoCatalogoService.obtenerPorId(99L)).thenReturn(Optional.empty());
        assertThat(controller.obtenerPremontado(99L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void chatDelegaEnChatService() {
        ChatRequest request = new ChatRequest("hola", List.of());
        when(chatService.generarRespuestaChat("hola", List.of())).thenReturn("respuesta");
        var http = controller.chatCatalogo(request);
        assertThat(http.getBody().respuesta()).isEqualTo("respuesta");
    }

    private PremontadoCatalogoDto dto() {
        return new PremontadoCatalogoDto(1L, "Asus ROG", "desc", "Asus", 10, "WINDOWS", 5,
                List.of("GAMING"), null, false, 1170.0, 1053.0, 4.5, 2, false, 80.0, List.of(), 3L);
    }
}

package com.optimapc.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.optimapc.backend.catalogo.dto.FavoritoDto;

@ExtendWith(MockitoExtension.class)
class FavoritoControllerTest {

    @Mock
    private FavoritoService favoritoService;
    @Mock
    private Authentication authentication;

    private FavoritoController controller;

    @BeforeEach
    void setUp() {
        controller = new FavoritoController(favoritoService);
        when(authentication.getPrincipal()).thenReturn(7L);
    }

    @Test
    void anadirDevuelve201() {
        FavoritoDto dto = new FavoritoDto(1L, null, null);
        when(favoritoService.añadir(7L, 1L)).thenReturn(dto);
        var http = controller.añadir(authentication, 1L);
        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(http.getBody()).isEqualTo(dto);
    }

    @Test
    void eliminarDevuelve204() {
        var http = controller.eliminar(authentication, 1L);
        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(favoritoService).eliminar(7L, 1L);
    }

    @Test
    void listarDevuelveFavoritos() {
        when(favoritoService.listarDeUsuario(7L)).thenReturn(List.of());
        assertThat(controller.listar(authentication).getBody()).isEmpty();
    }

    @Test
    void estadoDevuelveBooleano() {
        when(favoritoService.esFavorito(7L, 1L)).thenReturn(true);
        assertThat(controller.estado(authentication, 1L).getBody()).isTrue();
    }
}

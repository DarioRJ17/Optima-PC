package com.optimapc.backend.montarPC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.optimapc.backend.montarPC.dto.GuardarConfiguracionNombradaRequest;
import com.optimapc.backend.montarPC.dto.MiConfiguracionDto;

@ExtendWith(MockitoExtension.class)
class MisConfiguracionesControllerTest {

    @Mock
    private MisConfiguracionesService service;
    @Mock
    private Authentication authentication;

    private MisConfiguracionesController controller;

    @BeforeEach
    void setUp() {
        controller = new MisConfiguracionesController(service);
        when(authentication.getPrincipal()).thenReturn(7L);
    }

    @Test
    void guardarUsaElUsuarioAutenticado() {
        GuardarConfiguracionNombradaRequest request = new GuardarConfiguracionNombradaRequest(List.of(1L), "Mi PC");
        MiConfiguracionDto dto = new MiConfiguracionDto(1L, "Mi PC", 500.0, null, List.of());
        when(service.guardar(7L, List.of(1L), "Mi PC")).thenReturn(dto);

        assertThat(controller.guardar(authentication, request)).isEqualTo(dto);
    }

    @Test
    void listarUsaElUsuarioAutenticado() {
        when(service.listar(7L)).thenReturn(List.of());
        assertThat(controller.listar(authentication)).isEmpty();
    }

    @Test
    void eliminarUsaElUsuarioAutenticado() {
        controller.eliminar(authentication, 3L);
        verify(service).eliminar(3L, 7L);
    }
}

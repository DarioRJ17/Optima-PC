package com.optimapc.backend.recomendacion;

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

import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.recomendacion.dto.EncuestaInicialRequest;

@ExtendWith(MockitoExtension.class)
class PerfilUsuarioControllerTest {

    @Mock
    private PerfilUsuarioService service;
    @Mock
    private Authentication authentication;

    private PerfilUsuarioController controller;

    @BeforeEach
    void setUp() {
        controller = new PerfilUsuarioController(service);
    }

    @Test
    void guardarEncuestaUsaUsuarioAutenticadoYDevuelve200() {
        when(authentication.getPrincipal()).thenReturn(7L);
        EncuestaInicialRequest request = new EncuestaInicialRequest(TipoUso.GAMING, List.of(), 1000.0, false);

        var http = controller.guardarEncuestaInicial(authentication, request);

        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).guardarEncuestaInicial(7L, request);
    }
}

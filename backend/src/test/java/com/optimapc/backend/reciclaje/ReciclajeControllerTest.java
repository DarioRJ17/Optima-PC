package com.optimapc.backend.reciclaje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.optimapc.backend.reciclaje.ReciclajeController.ReciclajeRequest;
import com.optimapc.backend.reciclaje.dto.ReciclajeTipoUsoDto;

@ExtendWith(MockitoExtension.class)
class ReciclajeControllerTest {

    @Mock
    private ReciclajeService reciclajeService;

    private ReciclajeController controller;

    @BeforeEach
    void setUp() {
        controller = new ReciclajeController(reciclajeService);
    }

    @Test
    void sugerirDevuelve200ConLasConfiguraciones() {
        ReciclajeTipoUsoDto dto = new ReciclajeTipoUsoDto("GAMING", List.of());
        when(reciclajeService.sugerirConfiguraciones(List.of(1L, 2L))).thenReturn(List.of(dto));

        var http = controller.sugerirConfiguraciones(new ReciclajeRequest(List.of(1L, 2L)));

        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.getBody()).containsExactly(dto);
    }
}

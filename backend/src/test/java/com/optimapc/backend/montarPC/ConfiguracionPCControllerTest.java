package com.optimapc.backend.montarPC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.optimapc.backend.catalogo.dto.ComponenteDto;
import com.optimapc.backend.montarPC.dto.ComponenteDetalleDto;
import com.optimapc.backend.montarPC.dto.ConfiguracionGuardadaDto;
import com.optimapc.backend.montarPC.dto.ConsumoDto;
import com.optimapc.backend.montarPC.dto.GuardarConfiguracionRequest;

@ExtendWith(MockitoExtension.class)
class ConfiguracionPCControllerTest {

    @Mock
    private MontarPCService montarPCService;

    private ConfiguracionPCController controller;

    @BeforeEach
    void setUp() {
        controller = new ConfiguracionPCController(montarPCService);
    }

    @Test
    void delegaListadoDeComponentes() {
        ComponenteDto dto = new ComponenteDto(1L, "procesador", "CPU", "spec", 100.0, 1);
        when(montarPCService.getAllComponents()).thenReturn(List.of(dto));
        assertThat(controller.getAllComponents()).containsExactly(dto);
    }

    @Test
    void delegaComponentesCompatibles() {
        when(montarPCService.getCompatibleComponents(List.of(1L))).thenReturn(List.of());
        assertThat(controller.getCompatibleComponents(List.of(1L))).isEmpty();
    }

    @Test
    void delegaComponentesCompatiblesConAvisos() {
        when(montarPCService.getCompatibleComponentsWithWarnings(null)).thenReturn(List.of());
        assertThat(controller.getCompatibleComponentsWithWarnings(null)).isEmpty();
    }

    @Test
    void detalleExistenteDevuelve200() {
        ComponenteDetalleDto dto = new ComponenteDetalleDto(1L, "procesador", "CPU", "spec", 100.0, 1, 65, java.util.Map.of());
        when(montarPCService.getComponenteDetalle(1L)).thenReturn(Optional.of(dto));
        ResponseEntity<ComponenteDetalleDto> respuesta = controller.getComponenteDetalle(1L);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(dto);
    }

    @Test
    void detalleInexistenteDevuelve404() {
        when(montarPCService.getComponenteDetalle(99L)).thenReturn(Optional.empty());
        assertThat(controller.getComponenteDetalle(99L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delegaConsumo() {
        ConsumoDto consumo = new ConsumoDto(100, 125, 650, 420, true);
        when(montarPCService.calcularConsumo(null)).thenReturn(consumo);
        assertThat(controller.getConsumo(null)).isEqualTo(consumo);
    }

    @Test
    void delegaEquilibrio() {
        EquilibrioResult resultado = new EquilibrioResult(80.0, List.of());
        when(montarPCService.calcularEquilibrio(null)).thenReturn(resultado);
        assertThat(controller.getEquilibrio(null)).isEqualTo(resultado);
    }

    @Test
    void delegaGuardarConfiguracion() {
        ConfiguracionGuardadaDto guardada = new ConfiguracionGuardadaDto(5L, 999.0);
        when(montarPCService.guardarConfiguracion(List.of(1L, 2L))).thenReturn(guardada);
        assertThat(controller.guardarConfiguracion(new GuardarConfiguracionRequest(List.of(1L, 2L)))).isEqualTo(guardada);
    }
}

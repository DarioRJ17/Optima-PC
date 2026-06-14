package com.optimapc.backend.reciclaje;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.procesador;
import static com.optimapc.backend.support.TestData.ram;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.montarPC.ComponenteRepository;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.reciclaje.dto.ReciclajeTipoUsoDto;

@ExtendWith(MockitoExtension.class)
class ReciclajeServiceTest {

    @Mock
    private ComponenteRepository componenteRepository;

    private ReciclajeService service;

    private List<Componente> catalogo;

    @BeforeEach
    void setUp() {
        service = new ReciclajeService(componenteRepository, new RendimientoService());
        catalogo = List.of(
                procesador(1L, "AM5", 16, 5.5, 105, 350.0),
                procesador(2L, "AM5", 8, 4.8, 65, 180.0),
                gpu(3L, 16, 2600, 900.0),
                gpu(4L, 8, 2100, 350.0),
                ram(5L, "DDR5", 2, 16, 6000, 150.0),
                ram(6L, "DDR5", 2, 8, 5200, 90.0),
                almacenamiento(7L, "SSD", "M.2 NVMe", 2000, 130.0),
                almacenamiento(8L, "SSD", "SATA", 1000, 70.0));
    }

    @Test
    void devuelveUnaSugerenciaPorCadaTipoDeUso() {
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of()); // sin piezas fijas
        when(componenteRepository.findAll()).thenReturn(catalogo);

        List<ReciclajeTipoUsoDto> resultado = service.sugerirConfiguraciones(List.of());

        assertThat(resultado).hasSize(TipoUso.values().length);
        assertThat(resultado).extracting(ReciclajeTipoUsoDto::tipoUso)
                .containsExactlyInAnyOrder("GAMING", "OFIMATICA", "EDICION", "PROGRAMACION", "STREAMING");
    }

    @Test
    void rellenaHuecosPartiendoDeUnaPiezaFija() {
        Componente cpuFija = procesador(1L, "AM5", 16, 5.5, 105, 350.0);
        when(componenteRepository.findAllById(List.of(1L))).thenReturn(List.of(cpuFija));
        when(componenteRepository.findAll()).thenReturn(catalogo);

        List<ReciclajeTipoUsoDto> resultado = service.sugerirConfiguraciones(List.of(1L));

        assertThat(resultado).hasSize(TipoUso.values().length);
        // Para gaming, con buen catálogo, debería poder proponer al menos una configuración
        ReciclajeTipoUsoDto gaming = resultado.stream()
                .filter(r -> r.tipoUso().equals("GAMING")).findFirst().orElseThrow();
        assertThat(gaming.configuraciones()).isNotEmpty();
        // La CPU fija debe aparecer marcada como fija en las sugerencias
        assertThat(gaming.configuraciones().get(0).componentes())
                .anySatisfy(c -> assertThat(c.esFijo()).isTrue());
    }
}

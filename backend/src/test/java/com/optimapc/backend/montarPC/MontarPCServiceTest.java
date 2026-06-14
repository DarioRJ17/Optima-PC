package com.optimapc.backend.montarPC;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static com.optimapc.backend.support.TestData.caja;
import static com.optimapc.backend.support.TestData.fuente;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.placaBase;
import static com.optimapc.backend.support.TestData.procesador;
import static com.optimapc.backend.support.TestData.ram;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.catalogo.dto.ComponenteDto;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.RefrigeradorCPU;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.montarPC.dto.CompatibleComponenteDto;
import com.optimapc.backend.montarPC.dto.ComponenteDetalleDto;
import com.optimapc.backend.montarPC.dto.ConfiguracionGuardadaDto;
import com.optimapc.backend.montarPC.dto.ConsumoDto;
import com.optimapc.backend.pedido.ConfiguracionPCRepository;

@ExtendWith(MockitoExtension.class)
class MontarPCServiceTest {

    @Mock
    private ComponenteRepository componenteRepository;
    @Mock
    private ConfiguracionPCRepository configuracionPCRepository;

    private MontarPCService service;

    private Componente cpu, mobo, memoria, tarjeta, disco, psu, torre, refri;

    @BeforeEach
    void setUp() {
        service = new MontarPCService(componenteRepository, new RendimientoService(), configuracionPCRepository);
        cpu = procesador(1L, "AM5", 8, 5.0, 105, 300.0);
        mobo = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        memoria = ram(3L, "DDR5", 2, 16, 6000, 150.0);
        tarjeta = gpu(4L, 12, 2500, 600.0);
        disco = almacenamiento(5L, "SSD", "M.2 NVMe", 2000, 120.0);
        psu = fuente(6L, 750, 90.0);
        torre = caja(7L, "ATX", 80.0);
        RefrigeradorCPU r = new RefrigeradorCPU();
        r.setId(8L);
        r.setNombre("Refri");
        r.setPrecio(60.0);
        r.setRpmMin(800);
        r.setRpmMax(1800);
        r.setNivelRuidoMin(20.0);
        r.setNivelRuidoMax(35.0);
        r.setConsumoWatts(8);
        refri = r;
    }

    private List<Componente> catalogoCompleto() {
        return List.of(cpu, mobo, memoria, tarjeta, disco, psu, torre, refri);
    }

    @Test
    void devuelveTodosLosComponentesComoDto() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        List<ComponenteDto> dtos = service.getAllComponents();
        assertThat(dtos).hasSize(8);
        assertThat(dtos).extracting(ComponenteDto::tipo)
                .contains("procesador", "placa-base", "memoria-ram", "tarjeta-grafica",
                        "almacenamiento", "fuente-alimentacion", "caja", "refrigerador-cpu");
    }

    @Test
    void componentesCompatiblesSinSeleccionDevuelveTodos() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        assertThat(service.getCompatibleComponents(null)).hasSize(8);
        assertThat(service.getCompatibleComponents(List.of())).hasSize(8);
    }

    @Test
    void componentesCompatiblesConSeleccionFiltraIncompatibles() {
        when(componenteRepository.findAllById(List.of(2L))).thenReturn(List.of(mobo));
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        List<ComponenteDto> compatibles = service.getCompatibleComponents(List.of(2L));
        // La placa AM5/DDR5 ya seleccionada permanece y la CPU AM5 es compatible
        assertThat(compatibles).extracting(ComponenteDto::id).contains(1L, 2L);
    }

    @Test
    void componentesCompatiblesConAvisosIncluyenPropiedades() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        List<CompatibleComponenteDto> dtos = service.getCompatibleComponentsWithWarnings(null);
        assertThat(dtos).hasSize(8);
        assertThat(dtos).allSatisfy(d -> assertThat(d.propiedades()).isNotEmpty());
    }

    @Test
    void componentesCompatiblesConAvisosYSeleccion() {
        when(componenteRepository.findAllById(List.of(2L))).thenReturn(List.of(mobo));
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        assertThat(service.getComponentsByTypeWithWarnings("procesador", List.of(2L)))
                .extracting(CompatibleComponenteDto::id).contains(1L);
    }

    @Test
    void componentesPorTipoFiltraPorCategoria() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        assertThat(service.getComponentsByType("memoria-ram", null))
                .extracting(ComponenteDto::tipo).containsOnly("memoria-ram");
    }

    @Test
    void componentesPorTipoConAvisosSinSeleccion() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        assertThat(service.getComponentsByTypeWithWarnings("caja", null))
                .extracting(CompatibleComponenteDto::tipo).containsOnly("caja");
    }

    @Test
    void componentesPorTipoConSeleccionAplicaCompatibilidad() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        when(componenteRepository.findAllById(List.of(1L))).thenReturn(List.of(cpu));
        // Para una CPU AM5 seleccionada, solo placas AM5 son compatibles
        assertThat(service.getComponentsByType("placa-base", List.of(1L)))
                .extracting(ComponenteDto::id).contains(2L);
    }

    @Test
    void tiposDisponiblesSonLosOchoEsperados() {
        assertThat(service.getAvailableTypes()).hasSize(8).contains("procesador", "refrigerador-cpu");
    }

    @Test
    void detalleDeComponenteExistente() {
        when(componenteRepository.findById(4L)).thenReturn(Optional.of(tarjeta));
        Optional<ComponenteDetalleDto> detalle = service.getComponenteDetalle(4L);
        assertThat(detalle).isPresent();
        assertThat(detalle.get().tipo()).isEqualTo("tarjeta-grafica");
        assertThat(detalle.get().detalles()).containsKey("fabricante");
    }

    @Test
    void detalleDeComponenteInexistente() {
        when(componenteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.getComponenteDetalle(99L)).isEmpty();
    }

    @Test
    void detalleInfiereFabricantePorChipset() {
        TarjetaGrafica nvidia = gpu(10L, 12, 2500, 600.0);
        nvidia.setChipset("GeForce RTX 4070");
        TarjetaGrafica amd = gpu(11L, 12, 2500, 600.0);
        amd.setChipset("Radeon RX 7800");
        TarjetaGrafica intel = gpu(12L, 12, 2500, 600.0);
        intel.setChipset("Arc A770");
        TarjetaGrafica otro = gpu(13L, 12, 2500, 600.0);
        otro.setChipset(null);

        when(componenteRepository.findById(10L)).thenReturn(Optional.of(nvidia));
        when(componenteRepository.findById(11L)).thenReturn(Optional.of(amd));
        when(componenteRepository.findById(12L)).thenReturn(Optional.of(intel));
        when(componenteRepository.findById(13L)).thenReturn(Optional.of(otro));

        assertThat(service.getComponenteDetalle(10L).get().detalles().get("fabricante")).isEqualTo("NVIDIA");
        assertThat(service.getComponenteDetalle(11L).get().detalles().get("fabricante")).isEqualTo("AMD");
        assertThat(service.getComponenteDetalle(12L).get().detalles().get("fabricante")).isEqualTo("Intel");
        assertThat(service.getComponenteDetalle(13L).get().detalles().get("fabricante")).isEqualTo("Otro");
    }

    @Test
    void consumoSinSeleccionEsCero() {
        ConsumoDto consumo = service.calcularConsumo(null);
        assertThat(consumo.consumoEstimadoW()).isZero();
        assertThat(consumo.suficiente()).isTrue();
    }

    @Test
    void consumoConFuenteCalculaDisponibleYSuficiencia() {
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of(cpu, tarjeta, psu));
        ConsumoDto consumo = service.calcularConsumo(List.of(1L, 4L, 6L));
        assertThat(consumo.consumoEstimadoW()).isPositive();
        assertThat(consumo.potenciaPSUW()).isEqualTo(750);
        assertThat(consumo.suficiente()).isTrue();
        assertThat(consumo.disponibleW()).isNotNull();
    }

    @Test
    void equilibrioSinSeleccionEsCero() {
        EquilibrioResult resultado = service.calcularEquilibrio(List.of());
        assertThat(resultado.score()).isZero();
        assertThat(resultado.componentes()).isEmpty();
    }

    @Test
    void equilibrioConSeleccionDelegaEnRendimiento() {
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of(cpu, tarjeta, memoria, disco));
        EquilibrioResult resultado = service.calcularEquilibrio(List.of(1L, 4L, 3L, 5L));
        assertThat(resultado.score()).isBetween(0.0, 100.0);
    }

    @Test
    void guardarConfiguracionConComponentesPersiste() {
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of(cpu, mobo));
        when(configuracionPCRepository.save(any(ConfiguracionPC.class))).thenAnswer(inv -> {
            ConfiguracionPC c = inv.getArgument(0);
            c.setId(42L);
            return c;
        });
        ConfiguracionGuardadaDto dto = service.guardarConfiguracion(List.of(1L, 2L));
        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.precio()).isEqualTo(500.0);
    }

    @Test
    void guardarConfiguracionSinComponentesLanzaBadRequest() {
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service.guardarConfiguracion(List.of(99L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se encontraron");
    }
}

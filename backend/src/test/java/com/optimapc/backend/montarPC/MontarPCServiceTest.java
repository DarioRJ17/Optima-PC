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
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.catalogo.dto.ComponenteDto;
import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Caja;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.FuenteAlimentacion;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.PlacaBase;
import com.optimapc.backend.domain.Procesador;
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

    @Test
    void especificacionesIncluyenCamposOpcionalesCuandoEstanPresentes() {
        TarjetaGrafica g = gpu(1L, 12, 2500, 600.0);
        g.setLongitud(320);
        g.setConsumoWatts(220);
        Procesador p = procesador(2L, "AM5", 8, 5.0, 105, 300.0);
        p.setConsumoWatts(110); // con consumoWatts -> no usa la rama del tdp
        MemoriaRAM r = ram(3L, "DDR5", 2, 16, 6000, 150.0);
        r.setConsumoWatts(10);
        Almacenamiento a = almacenamiento(4L, "SSD", "M.2 NVMe", 2000, 120.0);
        a.setConsumoWatts(7);
        PlacaBase pb = placaBase(5L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        pb.setConsumoWatts(15);
        FuenteAlimentacion fa = fuente(6L, 750, 90.0);
        fa.setConsumoWatts(40);
        Caja c = caja(7L, "ATX", 80.0);
        c.setPanelLateral("Cristal");
        c.setConsumoWatts(5);

        when(componenteRepository.findAll()).thenReturn(List.of(g, p, r, a, pb, fa, c));

        List<ComponenteDto> dtos = service.getAllComponents();

        assertThat(dtos).extracting(ComponenteDto::especificacion).allMatch(s -> s.contains("W"));
        assertThat(dtos).extracting(ComponenteDto::especificacion)
                .anyMatch(s -> s.contains("mm"))            // longitud de la GPU
                .anyMatch(s -> s.contains("Panel lateral")); // panel lateral de la caja
    }

    @Test
    void especificacionConFrecuenciasParcialesONulas() {
        TarjetaGrafica soloBoost = gpu(1L, 8, 2000, 400.0);
        soloBoost.setFrecuenciaBase(null); // base null, boost presente
        TarjetaGrafica sinFrecuencias = gpu(2L, 8, 2000, 400.0);
        sinFrecuencias.setFrecuenciaBase(null);
        sinFrecuencias.setFrecuenciaBoost(null); // ambas null
        Procesador soloBase = procesador(3L, "AM5", 8, 5.0, 105, 300.0);
        soloBase.setFrecuenciaBoost(null); // base presente, boost null

        when(componenteRepository.findAll()).thenReturn(List.of(soloBoost, sinFrecuencias, soloBase));

        assertThat(service.getAllComponents()).hasSize(3);
    }

    @Test
    void detalleRefrigeradorConRangosParcialesONulos() {
        RefrigeradorCPU sinRangos = new RefrigeradorCPU();
        sinRangos.setId(20L);
        sinRangos.setNombre("Refri2");
        sinRangos.setPrecio(40.0); // sin rpm ni ruido -> rango ""
        RefrigeradorCPU rangoIgual = new RefrigeradorCPU();
        rangoIgual.setId(21L);
        rangoIgual.setNombre("Refri3");
        rangoIgual.setPrecio(45.0);
        rangoIgual.setRpmMin(1200);
        rangoIgual.setRpmMax(1200); // max == min -> "1200"
        RefrigeradorCPU soloMax = new RefrigeradorCPU();
        soloMax.setId(22L);
        soloMax.setNombre("Refri4");
        soloMax.setPrecio(50.0);
        soloMax.setRpmMax(1500); // rpmMin null, rpmMax presente -> "1500"

        when(componenteRepository.findById(20L)).thenReturn(Optional.of(sinRangos));
        when(componenteRepository.findById(21L)).thenReturn(Optional.of(rangoIgual));
        when(componenteRepository.findById(22L)).thenReturn(Optional.of(soloMax));

        assertThat(service.getComponenteDetalle(20L).get().detalles().get("rpm")).isNull();
        assertThat(service.getComponenteDetalle(21L).get().detalles().get("rpm")).isEqualTo("1200 RPM");
        assertThat(service.getComponenteDetalle(22L).get().detalles().get("rpm")).isEqualTo("1500 RPM");
    }

    @Test
    void componentesPorTipoParaCadaCategoriaYTipoDesconocido() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());

        for (String tipo : service.getAvailableTypes()) {
            assertThat(service.getComponentsByType(tipo, null))
                    .allSatisfy(d -> assertThat(d.tipo()).isEqualTo(tipo));
        }

        assertThat(service.getComponentsByType("tipo-inexistente", null)).isEmpty();
    }

    @Test
    void consumoSinFuenteNoCalculaDisponibleYEsSuficiente() {
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of(cpu, tarjeta));
        ConsumoDto consumo = service.calcularConsumo(List.of(1L, 4L));
        assertThat(consumo.potenciaPSUW()).isNull();
        assertThat(consumo.disponibleW()).isNull();
        assertThat(consumo.suficiente()).isTrue();
    }

    @Test
    void consumoConFuenteDebilEsInsuficiente() {
        var fuenteDebil = fuente(9L, 100, 40.0);
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of(cpu, tarjeta, fuenteDebil));
        ConsumoDto consumo = service.calcularConsumo(List.of(1L, 4L, 9L));
        assertThat(consumo.suficiente()).isFalse();
    }

    @Test
    void inferirFabricantePorPalabrasClaveAlternativas() {
        assertThat(detalleConChipset(30L, "GTX 1660").get("fabricante")).isEqualTo("NVIDIA");
        assertThat(detalleConChipset(31L, "Vega 64").get("fabricante")).isEqualTo("AMD");
        assertThat(detalleConChipset(32L, "Iris Xe").get("fabricante")).isEqualTo("Intel");
    }

    private Map<String, Object> detalleConChipset(Long id, String chipset) {
        TarjetaGrafica g = gpu(id, 8, 2000, 400.0);
        g.setChipset(chipset);
        when(componenteRepository.findById(id)).thenReturn(Optional.of(g));
        return service.getComponenteDetalle(id).get().detalles();
    }

    // --- Filtrado con selección: descarta candidatos incompatibles en los cuatro métodos ---

    @Test
    void compatiblesSinAvisosDescartaIncompatibles() {
        Componente cpuIntel = procesador(9L, "LGA1700", 8, 5.0, 105, 300.0);
        when(componenteRepository.findAllById(List.of(2L))).thenReturn(List.of(mobo));
        when(componenteRepository.findAll()).thenReturn(List.of(cpu, mobo, cpuIntel));
        assertThat(service.getCompatibleComponents(List.of(2L)))
                .extracting(ComponenteDto::id).contains(1L, 2L).doesNotContain(9L);
    }

    @Test
    void compatiblesConAvisosYSeleccionDescartaIncompatibles() {
        Componente cpuIntel = procesador(9L, "LGA1700", 8, 5.0, 105, 300.0);
        when(componenteRepository.findAllById(List.of(2L))).thenReturn(List.of(mobo));
        when(componenteRepository.findAll()).thenReturn(List.of(cpu, mobo, cpuIntel));
        assertThat(service.getCompatibleComponentsWithWarnings(List.of(2L)))
                .extracting(CompatibleComponenteDto::id).contains(1L, 2L).doesNotContain(9L);
    }

    @Test
    void porTipoConSeleccionDescartaIncompatibles() {
        Componente cpuIntel = procesador(9L, "LGA1700", 8, 5.0, 105, 300.0);
        when(componenteRepository.findAllById(List.of(2L))).thenReturn(List.of(mobo));
        when(componenteRepository.findAll()).thenReturn(List.of(cpu, cpuIntel, mobo));
        assertThat(service.getComponentsByType("procesador", List.of(2L)))
                .extracting(ComponenteDto::id).contains(1L).doesNotContain(9L);
    }

    @Test
    void porTipoConAvisosYSeleccionDescartaIncompatibles() {
        Componente cpuIntel = procesador(9L, "LGA1700", 8, 5.0, 105, 300.0);
        when(componenteRepository.findAllById(List.of(2L))).thenReturn(List.of(mobo));
        when(componenteRepository.findAll()).thenReturn(List.of(cpu, cpuIntel, mobo));
        assertThat(service.getComponentsByTypeWithWarnings("procesador", List.of(2L)))
                .extracting(CompatibleComponenteDto::id).contains(1L).doesNotContain(9L);
    }

    // --- Ramas de campos opcionales y guardas vacío/null ---

    @Test
    void especificacionDeProcesadorSinConsumoNiTdp() {
        Procesador p = procesador(1L, "AM5", 8, 5.0, 105, 300.0);
        p.setConsumoWatts(null);
        p.setTdp(null);
        when(componenteRepository.findAll()).thenReturn(List.of((Componente) p));
        assertThat(service.getAllComponents()).hasSize(1);
    }

    @Test
    void especificacionRefrigeradorConPartesSueltas() {
        RefrigeradorCPU soloConsumo = new RefrigeradorCPU();
        soloConsumo.setId(1L);
        soloConsumo.setNombre("Refri1");
        soloConsumo.setPrecio(50.0);
        soloConsumo.setConsumoWatts(5); // sin rpm ni ruido -> res vacío antes del consumo
        RefrigeradorCPU soloRuido = new RefrigeradorCPU();
        soloRuido.setId(2L);
        soloRuido.setNombre("Refri2");
        soloRuido.setPrecio(55.0);
        soloRuido.setNivelRuidoMin(20.0);
        soloRuido.setNivelRuidoMax(35.0); // sin rpm -> ruido se añade con res vacío

        when(componenteRepository.findAll()).thenReturn(List.of(soloConsumo, soloRuido));

        assertThat(service.getAllComponents())
                .extracting(ComponenteDto::especificacion)
                .anyMatch(s -> s.equals("5 W"))
                .anyMatch(s -> s.contains("dBA"));
    }

    @Test
    void consumoConListaVaciaEsCero() {
        assertThat(service.calcularConsumo(List.of()).consumoEstimadoW()).isZero();
    }

    @Test
    void equilibrioConIdsNulosEsCero() {
        assertThat(service.calcularEquilibrio(null).score()).isZero();
    }

    @Test
    void porTipoConTipoNuloDevuelveVacio() {
        when(componenteRepository.findAll()).thenReturn(catalogoCompleto());
        assertThat(service.getComponentsByType(null, null)).isEmpty();
    }
}

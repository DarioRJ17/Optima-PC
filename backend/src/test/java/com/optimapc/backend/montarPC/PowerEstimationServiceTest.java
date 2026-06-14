package com.optimapc.backend.montarPC;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static com.optimapc.backend.support.TestData.caja;
import static com.optimapc.backend.support.TestData.fuente;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.placaBase;
import static com.optimapc.backend.support.TestData.procesador;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.optimapc.backend.domain.Caja;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.RefrigeradorCPU;

class PowerEstimationServiceTest {

    @Test
    void componenteNuloConsumeCero() {
        assertThat(PowerEstimationService.estimateConsumption(null)).isZero();
    }

    @Test
    void fuenteAlimentacionNoSeContabilizaComoConsumidor() {
        assertThat(PowerEstimationService.estimateConsumption(fuente(1L, 650, 80.0))).isZero();
    }

    @Test
    void consumoWattsExplicitoTienePrioridad() {
        Caja caja = caja(1L, "ATX", 50.0);
        caja.setConsumoWatts(120);
        assertThat(PowerEstimationService.estimateConsumption(caja)).isEqualTo(120);
    }

    @Test
    void procesadorUsaTdpCuandoNoHayConsumoExplicito() {
        Procesador cpu = procesador(1L, "AM5", 8, 5.0, 105, 300.0);
        assertThat(PowerEstimationService.estimateConsumption(cpu)).isEqualTo(105);
    }

    @Test
    void procesadorSinTdpUsaValorPorDefecto() {
        Procesador cpu = procesador(1L, "AM5", 8, 5.0, null, 300.0);
        assertThat(PowerEstimationService.estimateConsumption(cpu)).isEqualTo(65);
    }

    @Test
    void tarjetaGraficaUsaHeuristicaMedia() {
        assertThat(PowerEstimationService.estimateConsumption(gpu(1L, 12, 2500, 600.0))).isEqualTo(150);
    }

    @Test
    void memoriaRamEscalaConNumeroDeModulos() {
        MemoriaRAM ram = new MemoriaRAM();
        ram.setNumModulos(4);
        assertThat(PowerEstimationService.estimateConsumption(ram)).isEqualTo(12);
    }

    @Test
    void memoriaRamSinModulosUsaValorPorDefecto() {
        assertThat(PowerEstimationService.estimateConsumption(new MemoriaRAM())).isEqualTo(6);
    }

    @Test
    void almacenamientoHddConsumeMasQueSsd() {
        assertThat(PowerEstimationService.estimateConsumption(almacenamiento(1L, "HDD", "SATA", 2000, 50.0))).isEqualTo(8);
        assertThat(PowerEstimationService.estimateConsumption(almacenamiento(2L, "SSD", "M.2", 1000, 80.0))).isEqualTo(5);
    }

    @Test
    void placaBaseYRefrigeradorYCajaTienenConsumoFijo() {
        assertThat(PowerEstimationService.estimateConsumption(placaBase(1L, "AM5", "DDR5", "ATX", 4, 128, 200.0))).isEqualTo(40);
        assertThat(PowerEstimationService.estimateConsumption(new RefrigeradorCPU())).isEqualTo(10);
        assertThat(PowerEstimationService.estimateConsumption(caja(1L, "ATX", 60.0))).isZero();
    }

    @Test
    void consumoTotalSumaTodosLosComponentes() {
        List<Componente> seleccion = List.of(
                procesador(1L, "AM5", 8, 5.0, 105, 300.0), // 105
                gpu(2L, 12, 2500, 600.0),                  // 150
                placaBase(3L, "AM5", "DDR5", "ATX", 4, 128, 200.0) // 40
        );
        assertThat(PowerEstimationService.estimateTotalWatts(seleccion)).isEqualTo(295);
    }

    @Test
    void consumoTotalDeListaNulaOVaciaEsCero() {
        assertThat(PowerEstimationService.estimateTotalWatts(null)).isZero();
        assertThat(PowerEstimationService.estimateTotalWatts(List.of())).isZero();
    }

    @Test
    void fuenteSuficienteAplicaMargen() {
        List<Componente> seleccion = List.of(procesador(1L, "AM5", 8, 5.0, 100, 300.0)); // 100W -> 125W con margen
        assertThat(PowerEstimationService.isPowerSufficient(fuente(2L, 200, 80.0), seleccion, 1.25)).isTrue();
        assertThat(PowerEstimationService.isPowerSufficient(fuente(3L, 120, 60.0), seleccion, 1.25)).isFalse();
    }

    @Test
    void fuenteNulaOSinPotenciaNoEsSuficiente() {
        List<Componente> seleccion = List.of(procesador(1L, "AM5", 8, 5.0, 100, 300.0));
        assertThat(PowerEstimationService.isPowerSufficient(null, seleccion, 1.25)).isFalse();
        assertThat(PowerEstimationService.isPowerSufficient(new com.optimapc.backend.domain.FuenteAlimentacion(), seleccion, 1.25)).isFalse();
    }
}

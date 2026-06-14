package com.optimapc.backend.montarPC;

import static com.optimapc.backend.support.TestData.caja;
import static com.optimapc.backend.support.TestData.fuente;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.placaBase;
import static com.optimapc.backend.support.TestData.procesador;
import static com.optimapc.backend.support.TestData.ram;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.PlacaBase;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.RefrigeradorCPU;
import com.optimapc.backend.montarPC.dto.CompatibilityWarningDto;

class CompatibilityServiceTest {

    // --- Procesador vs Placa base ---

    @Test
    void procesadorCompatibleConPlacaDelMismoSocket() {
        Procesador cpu = procesador(1L, "AM5", 8, 5.0, 105, 300.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(cpu, List.of(placa))).isTrue();
    }

    @Test
    void procesadorIncompatibleConPlacaDeOtroSocket() {
        Procesador cpu = procesador(1L, "AM5", 8, 5.0, 105, 300.0);
        PlacaBase placa = placaBase(2L, "LGA1700", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(cpu, List.of(placa))).isFalse();
    }

    @Test
    void procesadorSinSocketEsIncompatible() {
        Procesador cpu = procesador(1L, "AM5", 8, 5.0, 105, 300.0);
        cpu.setSocket(null);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(cpu, List.of(placa))).isFalse();
    }

    // --- Placa base candidata: restricciones acumulativas ---

    @Test
    void placaCompatibleConProcesadorRamYCaja() {
        PlacaBase placa = placaBase(1L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        Procesador cpu = procesador(2L, "AM5", 8, 5.0, 105, 300.0);
        MemoriaRAM memoria = ram(3L, "DDR5", 2, 16, 6000, 150.0);
        var cajaAtx = caja(4L, "ATX", 80.0);
        assertThat(CompatibilityService.isCompatible(placa, List.of(cpu, memoria, cajaAtx))).isTrue();
    }

    @Test
    void placaIncompatibleSiProcesadorNoCoincide() {
        PlacaBase placa = placaBase(1L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        Procesador cpu = procesador(2L, "LGA1700", 8, 5.0, 105, 300.0);
        assertThat(CompatibilityService.isCompatible(placa, List.of(cpu))).isFalse();
    }

    @Test
    void placaIncompatibleSiRamEsDeOtraGeneracionDDR() {
        PlacaBase placa = placaBase(1L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        MemoriaRAM memoria = ram(2L, "DDR4", 2, 16, 3200, 100.0);
        assertThat(CompatibilityService.isCompatible(placa, List.of(memoria))).isFalse();
    }

    @Test
    void placaSinRestriccionesEsCompatible() {
        PlacaBase placa = placaBase(1L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(placa, List.of())).isTrue();
    }

    // --- Memoria RAM vs Placa base ---

    @Test
    void ramCompatibleDentroDeLimites() {
        MemoriaRAM memoria = ram(1L, "DDR5", 2, 16, 6000, 150.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(memoria, List.of(placa))).isTrue();
    }

    @Test
    void ramIncompatiblePorExcederRanuras() {
        MemoriaRAM memoria = ram(1L, "DDR5", 4, 16, 6000, 150.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 2, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(memoria, List.of(placa))).isFalse();
    }

    @Test
    void ramIncompatiblePorExcederMemoriaMaxima() {
        MemoriaRAM memoria = ram(1L, "DDR5", 2, 32, 6000, 150.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 32, 200.0);
        assertThat(CompatibilityService.isCompatible(memoria, List.of(placa))).isFalse();
    }

    // --- Caja vs Placa base ---

    @Test
    void cajaAtxFullTowerAdmitePlacaAtx() {
        var cajaFull = caja(1L, "ATX Full Tower", 120.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatibleCajaVSPlaca(cajaFull, placa)).isTrue();
    }

    @Test
    void cajaMiniItxNoAdmitePlacaAtx() {
        var cajaMini = caja(1L, "Mini ITX", 90.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatibleCajaVSPlaca(cajaMini, placa)).isFalse();
    }

    @Test
    void candidataCajaSeValidaContraPlacaSeleccionada() {
        var cajaMini = caja(1L, "Mini ITX", 90.0);
        PlacaBase placa = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        assertThat(CompatibilityService.isCompatible(cajaMini, List.of(placa))).isFalse();
    }

    // --- Fuente de alimentación ---

    @Test
    void fuenteCandidataDebeAlimentarLaSeleccion() {
        Procesador cpu = procesador(1L, "AM5", 8, 5.0, 100, 300.0);
        assertThat(CompatibilityService.isCompatible(fuente(2L, 200, 80.0), List.of(cpu))).isTrue();
        assertThat(CompatibilityService.isCompatible(fuente(3L, 100, 60.0), List.of(cpu))).isFalse();
    }

    @Test
    void componenteAdicionalSeRechazaSiLaFuenteSeQuedaCorta() {
        var fuenteDebil = fuente(1L, 120, 60.0);
        Procesador cpu = procesador(2L, "AM5", 8, 5.0, 100, 300.0);
        var candidata = gpu(3L, 16, 2600, 1200.0); // +150W
        assertThat(CompatibilityService.isCompatible(candidata, List.of(fuenteDebil, cpu))).isFalse();
    }

    @Test
    void componenteSinReglasAplicablesEsCompatible() {
        RefrigeradorCPU refrigerador = new RefrigeradorCPU();
        assertThat(CompatibilityService.isCompatible(refrigerador, List.of())).isTrue();
    }

    // --- Avisos (warnings) de RAM ---

    @Test
    void avisaAlMezclarRamConDistintaFrecuenciaLatenciaYCapacidad() {
        MemoriaRAM candidata = ram(1L, "DDR5", 2, 16, 6000, 150.0);
        candidata.setLatenciaCAS(30);
        MemoriaRAM seleccionada = ram(2L, "DDR5", 2, 8, 5200, 100.0);
        seleccionada.setLatenciaCAS(40);
        List<Componente> seleccion = List.of(seleccionada);

        List<CompatibilityWarningDto> avisos = CompatibilityService.buildWarnings(candidata, seleccion);
        assertThat(avisos).extracting(CompatibilityWarningDto::code)
                .contains("ram_frequency_mismatch", "ram_latency_mismatch", "ram_capacity_mismatch");
    }

    @Test
    void sinRamSeleccionadaNoHayAvisos() {
        MemoriaRAM candidata = ram(1L, "DDR5", 2, 16, 6000, 150.0);
        assertThat(CompatibilityService.buildWarnings(candidata, List.of())).isEmpty();
    }

    @Test
    void componenteNoRamNoGeneraAvisos() {
        assertThat(CompatibilityService.buildWarnings(procesador(1L, "AM5", 8, 5.0, 105, 300.0), List.of())).isEmpty();
    }
}

package com.optimapc.backend.montarPC;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static com.optimapc.backend.support.TestData.caja;
import static com.optimapc.backend.support.TestData.config;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.procesador;
import static com.optimapc.backend.support.TestData.ram;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Caja;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoUso;

class RendimientoServiceTest {

    private RendimientoService service;

    @BeforeEach
    void setUp() {
        service = new RendimientoService();
    }

    @Test
    void configuracionVaciaTieneScoreCero() {
        assertThat(service.calcularScore(new ConfiguracionPC(), TipoUso.GAMING)).isZero();
    }

    @Test
    void scoreEstaEntreCeroYCien() {
        ConfiguracionPC c = config(
                procesador(1L, "AM5", 16, 5.5, 105, 500.0),
                gpu(2L, 16, 2600, 1200.0),
                ram(3L, "DDR5", 2, 16, 6000, 150.0),
                almacenamiento(4L, "SSD", "M.2 NVMe", 2000, 120.0));
        double score = service.calcularScore(c, TipoUso.GAMING);
        assertThat(score).isGreaterThan(0.0).isLessThanOrEqualTo(100.0);
    }

    @Test
    void tipoUsoNuloUsaPesosPorDefecto() {
        ConfiguracionPC c = config(procesador(1L, "AM5", 16, 5.5, 105, 500.0));
        assertThat(service.calcularScore(c, null)).isGreaterThan(0.0);
    }

    @Test
    void equilibrioDeConfiguracionVaciaEsCero() {
        ConfiguracionPC c = new ConfiguracionPC();
        EquilibrioResult resultado = service.calcularEquilibrio(c, TipoUso.GAMING);
        assertThat(resultado.score()).isZero();
        assertThat(resultado.componentes()).isEmpty();
        assertThat(c.getIndicadorEquilibrio()).isZero();
    }

    @Test
    void equilibrioDevuelveScoreAcotadoYRellenaIndicador() {
        ConfiguracionPC c = config(
                procesador(1L, "AM5", 16, 5.5, 105, 500.0),
                gpu(2L, 16, 2600, 1200.0),
                ram(3L, "DDR5", 2, 16, 6000, 150.0),
                almacenamiento(4L, "SSD", "M.2 NVMe", 2000, 120.0));
        EquilibrioResult resultado = service.calcularEquilibrio(c, TipoUso.GAMING);
        assertThat(resultado.score()).isBetween(0.0, 100.0);
        assertThat(c.getIndicadorEquilibrio()).isEqualTo(resultado.score());
    }

    @Test
    void equilibrioDetectaComponentesDesbalanceados() {
        // CPU muy potente y resto inexistente -> fuerte desviación
        ConfiguracionPC c = config(procesador(1L, "AM5", 24, 6.0, 105, 500.0));
        EquilibrioResult resultado = service.calcularEquilibrio(c, TipoUso.GAMING);
        assertThat(resultado.componentes()).isNotEmpty();
    }

    @Test
    void scoreNormalizadoDependeDelTipoDeComponente() {
        assertThat(service.scoreNormalizado(procesador(1L, "AM5", 16, 5.5, 105, 500.0))).isGreaterThan(0.0);
        assertThat(service.scoreNormalizado(gpu(2L, 16, 2600, 1200.0))).isGreaterThan(0.0);
        assertThat(service.scoreNormalizado(ram(3L, "DDR5", 2, 16, 6000, 150.0))).isGreaterThan(0.0);
        assertThat(service.scoreNormalizado(almacenamiento(4L, "SSD", "M.2 NVMe", 2000, 120.0))).isGreaterThan(0.0);
    }

    @Test
    void scoreNormalizadoDeOtroTipoEsCero() {
        Caja caja = new Caja();
        assertThat(service.scoreNormalizado(caja)).isZero();
    }

    @Test
    void interfazNvmeRindeMasQueSataYHdd() {
        double nvme = service.scoreNormalizado(almacenamiento(1L, "SSD", "M.2 NVMe", 1000, 100.0));
        Almacenamiento sata = almacenamiento(2L, "SSD", "SATA", 1000, 100.0);
        Almacenamiento hdd = almacenamiento(3L, "HDD", "SATA", 1000, 100.0);
        double scoreSata = service.scoreNormalizado(sata);
        double scoreHdd = service.scoreNormalizado(hdd);
        assertThat(nvme).isGreaterThan(scoreSata);
        assertThat(scoreSata).isGreaterThan(scoreHdd);
    }

    @Test
    void normalizarListaNulaOVaciaNoFalla() {
        service.normalizarLista(null);
        service.normalizarLista(List.of());
    }

    @Test
    void normalizarListaSinTipoUsoRellenaScores() {
        ConfiguracionPC a = config(gpu(1L, 16, 2600, 1000.0));
        ConfiguracionPC b = config(gpu(2L, 8, 2000, 500.0));
        service.normalizarLista(List.of(a, b));
        assertThat(a.getRendimientoPorEuro()).isBetween(0.0, 100.0);
        assertThat(b.getRendimientoPorEuro()).isBetween(0.0, 100.0);
    }

    @Test
    void normalizarListaConTipoUsoUsaMaximoDelTipo() {
        ConfiguracionPC delTipo = config(gpu(1L, 16, 2600, 1000.0));
        delTipo.getUsosPrevistos().add(TipoUso.GAMING);
        ConfiguracionPC otro = config(gpu(2L, 8, 2000, 500.0));
        service.normalizarLista(List.of(delTipo, otro), TipoUso.GAMING);
        assertThat(delTipo.getRendimientoPorEuro()).isBetween(0.0, 100.0);
        assertThat(otro.getRendimientoPorEuro()).isBetween(0.0, 100.0);
    }

    @Test
    void ignoraComponenteNuloYCantidadNula() {
        ConfiguracionPC c = new ConfiguracionPC();

        // Componente null -> se ignora en el bucle de sub-scores
        ConfiguracionComponente sinComponente = new ConfiguracionComponente();
        sinComponente.setCantidad(1);
        c.agregarComponente(sinComponente);

        // Cantidad null -> se interpreta como 1
        ConfiguracionComponente ramSinCantidad = new ConfiguracionComponente();
        ramSinCantidad.setComponente(ram(1L, "DDR5", 2, 16, 6000, 100.0));
        ramSinCantidad.setCantidad(null);
        c.agregarComponente(ramSinCantidad);

        assertThat(service.calcularScore(c, TipoUso.OFIMATICA)).isGreaterThan(0.0);
    }

    @Test
    void componenteNoPuntuableNoAportaScore() {
        // Una caja no es CPU/GPU/RAM/almacenamiento: ningún instanceof entra
        ConfiguracionPC c = config(caja(1L, "ATX", 50.0));
        assertThat(service.calcularScore(c, TipoUso.GAMING)).isZero();
    }

    @Test
    void componentesConCamposNulosNoRompenElCalculo() {
        Procesador cpu = new Procesador();
        cpu.setId(1L);
        cpu.setPrecio(100.0); // nucleos y frecuencias null

        TarjetaGrafica gpu = new TarjetaGrafica();
        gpu.setId(2L);
        gpu.setPrecio(100.0); // memoria y frecuencias null

        MemoriaRAM ram = new MemoriaRAM();
        ram.setId(3L);
        ram.setPrecio(50.0); // modulos/gb/velocidad null

        Almacenamiento sto = new Almacenamiento();
        sto.setId(4L);
        sto.setPrecio(50.0); // capacidad/interfaz/tipo null

        ConfiguracionPC c = config(cpu, gpu, ram, sto);
        assertThat(service.calcularScore(c, TipoUso.GAMING)).isZero();
    }

    @Test
    void gpuSinFrecuenciaBoostUsaLaFrecuenciaBase() {
        TarjetaGrafica gpu = new TarjetaGrafica();
        gpu.setId(1L);
        gpu.setPrecio(100.0);
        gpu.setMemoria(8);
        gpu.setFrecuenciaBase(2000); // boost null -> usa base
        assertThat(service.scoreNormalizado(gpu)).isGreaterThan(0.0);
    }

    @Test
    void normalizarListaConPrecioCeroAsignaRendimientoCero() {
        // Sin componentes -> precio 0 -> ratio 0 y máximo 0
        ConfiguracionPC sinPrecio = new ConfiguracionPC();
        service.normalizarLista(List.of(sinPrecio));
        assertThat(sinPrecio.getRendimientoPorEuro()).isZero();
    }
}

package com.optimapc.backend.reciclaje;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.procesador;
import static com.optimapc.backend.support.TestData.ram;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.reciclaje.dto.ReciclajeComponenteDto;
import com.optimapc.backend.reciclaje.dto.ReciclajeConfiguracionDto;
import com.optimapc.backend.reciclaje.dto.ReciclajeTipoUsoDto;

/**
 * Test de INTEGRACIÓN entre módulos: reciclaje ↔ configurador (montarPC).
 *
 * A diferencia del unitario (que mockea el repositorio), aquí colaboran las clases REALES
 * de dos módulos distintos sobre una BD real (H2):
 *   - reciclaje.ReciclajeService          (módulo bajo prueba)
 *   - montarPC.ComponenteRepository       (acceso a datos del configurador, contra H2)
 *   - montarPC.RendimientoService         (scoring y equilibrio del configurador)
 *
 * @DataJpaTest aporta el repositorio + H2 + TestEntityManager; @Import registra los dos
 * @Service para que se inyecten entre sí como en producción. Así verificamos que la costura
 * real entre módulos (reciclaje pidiendo componentes a BD y puntuándolos con el motor del
 * configurador) funciona de extremo a extremo.
 */
@DataJpaTest
@Import({RendimientoService.class, ReciclajeService.class})
class ReciclajeConfiguradorIntegrationTest {

    @Autowired
    private ReciclajeService reciclajeService;
    @Autowired
    private TestEntityManager em;

    /** Persiste un componente dejando que H2 genere el id (la estrategia es IDENTITY). */
    private <T extends Componente> T persist(T c) {
        c.setId(null);
        return em.persistAndFlush(c);
    }

    /** Catálogo variado, con candidatos de cada tipo dentro del nivel de una CPU de gama media-alta. */
    private void persistirCatalogo() {
        persist(gpu(null, 12, 2500, 600.0));
        persist(gpu(null, 8, 2100, 350.0));
        persist(ram(null, "DDR5", 2, 16, 6000, 150.0));
        persist(ram(null, "DDR5", 2, 8, 5200, 90.0));
        persist(almacenamiento(null, "SSD", "M.2 NVMe", 1000, 90.0));
        persist(almacenamiento(null, "SSD", "SATA", 1000, 60.0));
    }

    @Test
    void rellenaLosHuecosPartiendoDeUnaCpuFijaUsandoElMotorDelConfigurador() {
        Procesador cpuFija = persist(procesador(null, "AM5", 16, 5.5, 105, 350.0));
        persistirCatalogo();

        List<ReciclajeTipoUsoDto> resultado =
                reciclajeService.sugerirConfiguraciones(List.of(cpuFija.getId()));

        // Una entrada por cada tipo de uso (el reciclaje recorre todos los TipoUso)
        assertThat(resultado).hasSize(TipoUso.values().length);

        ReciclajeTipoUsoDto gaming = resultado.stream()
                .filter(r -> r.tipoUso().equals("GAMING")).findFirst().orElseThrow();

        // El motor del configurador (sobre datos reales de H2) ha producido sugerencias
        assertThat(gaming.configuraciones()).isNotEmpty().hasSizeLessThanOrEqualTo(3);

        ReciclajeConfiguracionDto mejor = gaming.configuraciones().get(0);

        // Cada sugerencia monta los 4 slots y la CPU del usuario va marcada como fija (esFijo)
        assertThat(mejor.componentes()).hasSize(4);
        assertThat(mejor.componentes())
                .filteredOn(ReciclajeComponenteDto::esFijo)
                .singleElement()
                .extracting(ReciclajeComponenteDto::id)
                .isEqualTo(cpuFija.getId());

        // El resto de piezas las sugiere el sistema (no fijas) y salen del catálogo persistido
        assertThat(mejor.componentes())
                .filteredOn(c -> !c.esFijo())
                .hasSize(3)
                .allSatisfy(c -> assertThat(c.id()).isNotNull());

        // Métricas calculadas por RendimientoService sobre la build real
        assertThat(mejor.precioTotal()).isGreaterThan(0.0);
        assertThat(mejor.scoreEquilibrio()).isBetween(0.0, 100.0);
    }

    @Test
    void preservaVariasPiezasFijasYSoloRellenaLosSlotsRestantes() {
        Procesador cpuFija = persist(procesador(null, "AM5", 16, 5.5, 105, 350.0));
        Componente gpuFija = persist(gpu(null, 12, 2500, 600.0));
        // Catálogo solo de RAM y almacenamiento: son los únicos huecos que el sistema debe rellenar
        persist(ram(null, "DDR5", 2, 16, 6000, 150.0));
        persist(ram(null, "DDR5", 2, 8, 5200, 90.0));
        persist(almacenamiento(null, "SSD", "M.2 NVMe", 1000, 90.0));
        persist(almacenamiento(null, "SSD", "SATA", 1000, 60.0));

        List<ReciclajeTipoUsoDto> resultado = reciclajeService.sugerirConfiguraciones(
                List.of(cpuFija.getId(), gpuFija.getId()));

        ReciclajeTipoUsoDto gaming = resultado.stream()
                .filter(r -> r.tipoUso().equals("GAMING")).findFirst().orElseThrow();
        assertThat(gaming.configuraciones()).isNotEmpty();

        // En toda sugerencia, las dos piezas del usuario se conservan marcadas como fijas...
        assertThat(gaming.configuraciones()).allSatisfy(config -> {
            assertThat(config.componentes())
                    .filteredOn(ReciclajeComponenteDto::esFijo)
                    .extracting(ReciclajeComponenteDto::id)
                    .containsExactlyInAnyOrder(cpuFija.getId(), gpuFija.getId());
            // ...y lo único que aporta el sistema son RAM y almacenamiento
            assertThat(config.componentes())
                    .filteredOn(c -> !c.esFijo())
                    .extracting(ReciclajeComponenteDto::categoria)
                    .containsExactlyInAnyOrder("RAM", "STORAGE");
        });
    }
}

package com.optimapc.backend.reciclaje;

import com.optimapc.backend.reciclaje.dto.ReciclajeConfiguracionDto;
import com.optimapc.backend.reciclaje.dto.ReciclajeComponenteDto;
import com.optimapc.backend.reciclaje.dto.ReciclajeTipoUsoDto;
import com.optimapc.backend.montarPC.EquilibrioResult;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.montarPC.ComponenteRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoUso;

@Service
public class ReciclajeService {

    // Límite de candidatos por slot no fijo para acotar el espacio de combinaciones
    private static final int MAX_CANDIDATOS_POR_SLOT = 10;
    private static final int MAX_SUGERENCIAS = 3;
    // Priorizamos el equilibrio sobre el rendimiento/€: para reciclar interesa más un equipo bien
    // proporcionado que uno barato pero descompensado.
    private static final double PESO_RENDIMIENTO = 0.4;
    private static final double PESO_EQUILIBRIO = 0.6;

    // Margen (en puntos de scoreNormalizado, escala 0-100) para considerar que una pieza está
    // "al mismo nivel" que las del usuario. Rellenamos los huecos con candidatos dentro de este
    // margen para no descompensar el equipo con gama muy superior o inferior.
    private static final double MARGEN_NIVEL = 20.0;

    // Suelo de equilibrio: si un componente aporta más de este umbral por debajo de lo esperado
    // (cuello de botella), la configuración se descarta. Evita que el rendimiento/€ premie builds
    // baratos pero desequilibrados (p. ej. una CPU de servidor floja junto a una buena GPU).
    private static final double UMBRAL_CUELLO = 0.15;

    private final ComponenteRepository componenteRepository;
    private final RendimientoService rendimientoService;

    public ReciclajeService(ComponenteRepository componenteRepository, RendimientoService rendimientoService) {
        this.componenteRepository = componenteRepository;
        this.rendimientoService = rendimientoService;
    }

    @Transactional(readOnly = true)
    public List<ReciclajeTipoUsoDto> sugerirConfiguraciones(List<Long> componenteIds) {
        List<Componente> fijos = componenteRepository.findAllById(componenteIds);
        List<Componente> catalogo = componenteRepository.findAll();

        return Arrays.stream(TipoUso.values())
                .map(tipoUso -> buscarOptimas(fijos, catalogo, tipoUso))
                .toList();
    }

    private ReciclajeTipoUsoDto buscarOptimas(List<Componente> fijos, List<Componente> catalogo, TipoUso tipoUso) {
        Set<Long> idsFijos = fijos.stream().map(Componente::getId).collect(Collectors.toSet());

        // Clasificar los componentes fijos por tipo
        Procesador cpuFijo = firstOf(fijos, Procesador.class);
        TarjetaGrafica gpuFija = firstOf(fijos, TarjetaGrafica.class);
        MemoriaRAM ramFija = firstOf(fijos, MemoriaRAM.class);
        Almacenamiento storageFijo = firstOf(fijos, Almacenamiento.class);

        // Nivel de referencia del equipo del usuario = media del scoreNormalizado de sus piezas.
        // Los huecos se rellenan con candidatos cercanos a ese nivel para mantener el equilibrio
        // (no meter gama muy superior ni inferior a lo que el usuario ya tiene).
        double nivelReferencia = fijos.stream()
                .mapToDouble(rendimientoService::scoreNormalizado)
                .average()
                .orElse(0.0);

        // Si el usuario aporta ese tipo, usarlo directamente; si no, tomar candidatos a su nivel
        List<Procesador> cpus = cpuFijo != null ? List.of(cpuFijo) : topDe(catalogo, Procesador.class, nivelReferencia);
        List<TarjetaGrafica> gpus = gpuFija != null ? List.of(gpuFija) : topDe(catalogo, TarjetaGrafica.class, nivelReferencia);
        List<MemoriaRAM> rams = ramFija != null ? List.of(ramFija) : topDe(catalogo, MemoriaRAM.class, nivelReferencia);
        List<Almacenamiento> storages = storageFijo != null ? List.of(storageFijo) : topDe(catalogo, Almacenamiento.class, nivelReferencia);

        // Construir todas las combinaciones candidatas
        List<ConfiguracionPC> candidatos = new ArrayList<>();
        for (Procesador cpu : cpus)
            for (TarjetaGrafica gpu : gpus)
                for (MemoriaRAM ram : rams)
                    for (Almacenamiento storage : storages)
                        candidatos.add(buildConfig(cpu, gpu, ram, storage));

        // Normalizar rendimiento/€ sobre el conjunto completo de candidatos
        rendimientoService.normalizarLista(candidatos, tipoUso);

        // Calcular equilibrio para cada candidato y guardar el resultado
        Map<ConfiguracionPC, EquilibrioResult> equilibrios = new IdentityHashMap<>();
        for (ConfiguracionPC config : candidatos) {
            equilibrios.put(config, rendimientoService.calcularEquilibrio(config, tipoUso));
        }

        // Suelo de equilibrio: descartar configuraciones con algún componente en cuello de botella
        // por encima del umbral (un eslabón demasiado débil para el resto). Si ninguna lo supera
        // (caso degenerado), se usan todas para no quedarnos sin sugerencias.
        List<ConfiguracionPC> aceptables = candidatos.stream()
                .filter(c -> !tieneCuelloGrave(equilibrios.get(c)))
                .toList();
        List<ConfiguracionPC> seleccionables = aceptables.isEmpty() ? candidatos : aceptables;

        // Seleccionar los mejores por score compuesto
        List<ConfiguracionPC> mejores = seleccionables.stream()
                .sorted(Comparator.comparingDouble(c ->
                        -(PESO_RENDIMIENTO * c.getRendimientoPorEuro() + PESO_EQUILIBRIO * c.getIndicadorEquilibrio())))
                .limit(MAX_SUGERENCIAS)
                .toList();

        List<ReciclajeConfiguracionDto> sugerencias = mejores.stream()
                .map(config -> toDto(config, idsFijos, equilibrios.get(config)))
                .toList();

        return new ReciclajeTipoUsoDto(tipoUso.name(), sugerencias);
    }

    // --- Selección de candidatos por tipo ---

    // Candidatos de un tipo para rellenar un hueco. Dos reglas:
    //  1) Tope por arriba: no superar el nivel de las piezas del usuario + MARGEN_NIVEL. Así no se
    //     sobredimensiona (nada de meter una 5090 con un Celeron) y se acota el equipo a su gama.
    //  2) Orden por relación calidad-precio (scoreNormalizado / precio), NO por potencia ni por
    //     cercanía. De este modo el pool abarca desde opciones modestas y baratas hasta las que
    //     llegan al tope, y es el score compuesto ponderado por tipo de uso quien decide:
    //       - donde el componente pesa mucho (GPU en gaming) tira hacia el nivel alto del pool,
    //       - donde pesa poco (GPU en ofimática) se queda con la opción barata.
    // Esto es lo que diferencia las sugerencias por tipo de uso sin tocar el indicador de equilibrio.
    private <T extends Componente> List<T> topDe(List<Componente> catalogo, Class<T> tipo, double nivelReferencia) {
        double tope = nivelReferencia + MARGEN_NIVEL;
        return catalogo.stream()
                .filter(tipo::isInstance).map(tipo::cast)
                .filter(c -> rendimientoService.scoreNormalizado(c) <= tope)
                .sorted(Comparator.comparingDouble((T c) -> valor(c)).reversed())
                .limit(MAX_CANDIDATOS_POR_SLOT)
                .toList();
    }

    // Relación calidad-precio de un componente: rendimiento normalizado por euro.
    private double valor(Componente c) {
        Double precio = c.getPrecio();
        if (precio == null || precio <= 0) return 0.0;
        return rendimientoService.scoreNormalizado(c) / precio;
    }

    // --- Construcción de configuraciones virtuales (no persistidas) ---

    private ConfiguracionPC buildConfig(Procesador cpu, TarjetaGrafica gpu, MemoriaRAM ram, Almacenamiento storage) {
        ConfiguracionPC config = new ConfiguracionPC();
        addComponente(config, cpu, "CPU");
        addComponente(config, gpu, "GPU");
        addComponente(config, ram, "RAM");
        addComponente(config, storage, "STORAGE");
        return config;
    }

    private void addComponente(ConfiguracionPC config, Componente c, String categoria) {
        if (c == null) return;
        ConfiguracionComponente cc = new ConfiguracionComponente();
        cc.setComponente(c);
        cc.setCategoria(categoria);
        cc.setCantidad(1);
        cc.setConfiguracion(config);
        config.getComponentes().add(cc);
    }

    // --- Mapeo a DTO ---

    private ReciclajeConfiguracionDto toDto(ConfiguracionPC config, Set<Long> idsFijos, EquilibrioResult equilibrio) {
        List<ReciclajeComponenteDto> componentes = config.getComponentes().stream()
                .map(cc -> new ReciclajeComponenteDto(
                        cc.getComponente().getId(),
                        cc.getComponente().getNombre(),
                        cc.getCategoria(),
                        cc.getComponente().getPrecio(),
                        cc.getCantidad(),
                        idsFijos.contains(cc.getComponente().getId())
                ))
                .toList();

        double scoreCompuesto = PESO_RENDIMIENTO * config.getRendimientoPorEuro()
                + PESO_EQUILIBRIO * config.getIndicadorEquilibrio();

        return new ReciclajeConfiguracionDto(
                componentes,
                redondear(config.getRendimientoPorEuro()),
                redondear(config.getIndicadorEquilibrio()),
                redondear(scoreCompuesto),
                redondear(config.getPrecio()),
                equilibrio.componentes()
        );
    }

    // True si algún componente está en cuello de botella (aporta menos de lo esperado) por encima
    // del umbral. Indica un eslabón demasiado débil para el resto del equipo.
    private boolean tieneCuelloGrave(EquilibrioResult equilibrio) {
        return equilibrio.componentes().stream()
                .anyMatch(d -> !d.sobredimensionado() && d.desviacion() > UMBRAL_CUELLO);
    }

    @SuppressWarnings("unchecked")
    private <T extends Componente> T firstOf(List<Componente> lista, Class<T> tipo) {
        return (T) lista.stream().filter(tipo::isInstance).findFirst().orElse(null);
    }

    private double redondear(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

package com.optimapc.backend.montarPC;

import com.optimapc.backend.montarPC.dto.ComponenteDesbalanceado;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoUso;

@Service
public class RendimientoService {

    // {w_cpu, w_gpu, w_ram, w_storage} — suman 1.0 por TipoUso
    private static final Map<TipoUso, double[]> PESOS = Map.of(
        TipoUso.GAMING,       new double[]{0.25, 0.50, 0.15, 0.10},
        TipoUso.OFIMATICA,    new double[]{0.45, 0.05, 0.30, 0.20},
        TipoUso.EDICION,      new double[]{0.30, 0.40, 0.20, 0.10},
        TipoUso.PROGRAMACION, new double[]{0.40, 0.05, 0.40, 0.15},
        TipoUso.STREAMING,    new double[]{0.40, 0.30, 0.20, 0.10}
    );

    // Media de todos los TipoUso: CPU 0.36, GPU 0.26, RAM 0.25, Storage 0.13
    private static final double[] PESOS_DEFAULT = {0.36, 0.26, 0.25, 0.13};

    // Desviación mínima para considerar un componente como problemático (5%)
    private static final double UMBRAL_DESVIACION = 0.05;

    // Valores de referencia (~gama alta de mercado) para escalar cada sub-score crudo a 0-100.
    // Como las fórmulas de cada categoría tienen magnitudes muy distintas (la capacidad de RAM
    // crece sin techo y aplastaría a CPU/GPU), dividimos por una referencia por categoría y
    // saturamos a 100. Así un componente tope de cada tipo vale ~100 y todos son comparables.
    // Son una calibración a ojo, ajustable; no pretenden ser exactos sino homogeneizar escalas.
    private static final double REF_CPU = 150;      // ~24 núcleos × 6,0 GHz
    private static final double REF_GPU = 15.0;     // √32 × 2655 MHz / 1000 ≈ RTX 5090
    private static final double REF_RAM = 288;      // 48 GB (capacidad útil máxima) a 6000 MHz
    private static final double REF_STORAGE = 12;   // 2 TB NVMe (capacidad útil máxima × 6 / 1000)

    // Topes de capacidad útil: más allá de esto, la capacidad extra no aporta rendimiento
    // perceptible en un PC de escritorio y solo serviría para "ganar" la métrica de forma irreal.
    private static final int CAP_RAM_GB = 48;
    private static final int CAP_STORAGE_GB = 2000;

    public double calcularScore(ConfiguracionPC config, TipoUso tipoUso) {
        double[] pesos = tipoUso != null ? PESOS.getOrDefault(tipoUso, PESOS_DEFAULT) : PESOS_DEFAULT;
        double[] sub = computeSubScores(config);
        return pesos[0] * sub[0] + pesos[1] * sub[1] + pesos[2] * sub[2] + pesos[3] * sub[3];
    }

    // Calcula el equilibrio de la configuración para un tipo de uso dado.
    // Mide cuánto se aleja la contribución real de cada componente de lo que el tipo de uso espera.
    public EquilibrioResult calcularEquilibrio(ConfiguracionPC config, TipoUso tipoUso) {
        double[] pesos = tipoUso != null ? PESOS.getOrDefault(tipoUso, PESOS_DEFAULT) : PESOS_DEFAULT;
        double[] sub = computeSubScores(config);

        double total = pesos[0] * sub[0] + pesos[1] * sub[1] + pesos[2] * sub[2] + pesos[3] * sub[3];
        if (total == 0) {
            config.setIndicadorEquilibrio(0.0);
            return new EquilibrioResult(0.0, List.of());
        }

        String[] nombres = {"CPU", "GPU", "RAM", "STORAGE"};
        List<ComponenteDesbalanceado> problematicos = new ArrayList<>();
        double sumaDesviaciones = 0;

        for (int i = 0; i < 4; i++) {
            double contribucion = (pesos[i] * sub[i]) / total;
            double desviacion = contribucion - pesos[i]; // positivo = sobredimensionado, negativo = cuello de botella
            sumaDesviaciones += Math.abs(desviacion);
            if (Math.abs(desviacion) >= UMBRAL_DESVIACION) {
                problematicos.add(new ComponenteDesbalanceado(nombres[i], desviacion > 0, Math.abs(desviacion)));
            }
        }

        // Ordenar de mayor a menor desviación para que el más problemático aparezca primero
        problematicos.sort(Comparator.comparingDouble(ComponenteDesbalanceado::desviacion).reversed());

        // sumaDesviaciones ∈ [0, 2] (contribuciones y pesos suman ambos 1), por eso dividimos
        // entre 2 para acotar el equilibrio a [0, 100]: 0 = máximo desequilibrio, 100 = perfecto.
        double score = (1 - sumaDesviaciones / 2.0) * 100;
        config.setIndicadorEquilibrio(score);

        return new EquilibrioResult(score, problematicos);
    }

    // Extrae los sub-scores de los cuatro tipos de componente, ya normalizados a 0-100.
    // Orden: [cpuScore, gpuScore, ramScore, storageScore]
    private double[] computeSubScores(ConfiguracionPC config) {
        double cpuRaw = 0, gpuRaw = 0, ramRaw = 0, storageRaw = 0;

        for (ConfiguracionComponente cc : config.getComponentes()) {
            Componente proxy = cc.getComponente();
            if (proxy == null) continue;
            // Con LAZY + JOINED inheritance, Hibernate devuelve un proxy de la clase base (Componente),
            // no del subtipo real. El instanceof fallaría sin hacer unproxy primero.
            var componente = Hibernate.unproxy(proxy);
            int cantidad = cc.getCantidad() != null ? cc.getCantidad() : 1;

            if (componente instanceof Procesador cpu) {
                cpuRaw = scoreCpu(cpu);
            } else if (componente instanceof TarjetaGrafica gpu) {
                gpuRaw = scoreGpu(gpu);
            } else if (componente instanceof MemoriaRAM ram) {
                ramRaw += scoreRam(ram) * cantidad;
            } else if (componente instanceof Almacenamiento storage) {
                storageRaw += scoreStorage(storage) * cantidad;
            }
        }

        // Escalar cada categoría a 0-100 con su referencia, para que sean comparables entre sí.
        return new double[]{
            normalizar(cpuRaw, REF_CPU),
            normalizar(gpuRaw, REF_GPU),
            normalizar(ramRaw, REF_RAM),
            normalizar(storageRaw, REF_STORAGE)
        };
    }

    // Escala un sub-score crudo a 0-100 según la referencia de su categoría, saturando en 100.
    private double normalizar(double valor, double ref) {
        return Math.min(100.0, (valor / ref) * 100.0);
    }

    // Score normalizado (0-100) de un componente individual, según su categoría.
    // Lo usa el módulo de reciclaje para preseleccionar los mejores candidatos de forma comparable.
    public double scoreNormalizado(Componente componente) {
        var c = Hibernate.unproxy(componente);
        if (c instanceof Procesador cpu) return normalizar(scoreCpu(cpu), REF_CPU);
        if (c instanceof TarjetaGrafica gpu) return normalizar(scoreGpu(gpu), REF_GPU);
        if (c instanceof MemoriaRAM ram) return normalizar(scoreRam(ram), REF_RAM);
        if (c instanceof Almacenamiento storage) return normalizar(scoreStorage(storage), REF_STORAGE);
        return 0;
    }

    // Normaliza usando pesos generales (media de todos los TipoUso). Para el catálogo.
    public void normalizarLista(List<? extends ConfiguracionPC> configs) {
        normalizarLista(configs, null);
    }

    // Normaliza usando pesos de un TipoUso específico. Para el módulo de reciclaje.
    public void normalizarLista(List<? extends ConfiguracionPC> configs, TipoUso tipoUso) {
        if (configs == null || configs.isEmpty()) return;

        List<Double> ratios = configs.stream().map(config -> {
            double score = calcularScore(config, tipoUso);
            config.setRendimientoScore(score);
            double precio = config.getPrecioEfectivo();
            return precio > 0 ? score / precio : 0.0;
        }).toList();

        // Si hay tipoUso, el max se calcula solo entre los premontados de ese tipo,
        // para no dejar que un PC de otra categoría más barato aplaste la escala.
        double max;
        if (tipoUso != null) {
            double typeMax = 0;
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i).getUsosPrevistos().contains(tipoUso)) {
                    typeMax = Math.max(typeMax, ratios.get(i));
                }
            }
            max = typeMax > 0 ? typeMax : ratios.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        } else {
            max = ratios.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        }

        for (int i = 0; i < configs.size(); i++) {
            double normalizado = max > 0 ? Math.min(100.0, (ratios.get(i) / max) * 100.0) : 0.0;
            configs.get(i).setRendimientoPorEuro(normalizado);
        }
    }

    private double scoreCpu(Procesador cpu) {
        int nucleos = cpu.getNucleos() != null ? cpu.getNucleos() : 0;
        double boost = cpu.getFrecuenciaBoost() != null ? cpu.getFrecuenciaBoost() : 0.0;
        return nucleos * boost;
    }

    private double scoreGpu(TarjetaGrafica gpu) {
        int memoria = gpu.getMemoria() != null ? gpu.getMemoria() : 0;
        int boost = gpu.getFrecuenciaBoost() != null ? gpu.getFrecuenciaBoost()
                  : (gpu.getFrecuenciaBase() != null ? gpu.getFrecuenciaBase() : 0);
        return (Math.sqrt(memoria) * boost) / 1000.0;
    }

    private double scoreRam(MemoriaRAM ram) {
        int modulos = ram.getNumModulos() != null ? ram.getNumModulos() : 0;
        int gbPorModulo = ram.getGbPorModulo() != null ? ram.getGbPorModulo() : 0;
        int velocidad = ram.getVelocidad() != null ? ram.getVelocidad() : 0;
        // Capamos la capacidad total para que una RAM gigante (256 GB) no aplaste a una de gama alta normal.
        int capacidadUtil = Math.min(modulos * gbPorModulo, CAP_RAM_GB);
        return capacidadUtil * (velocidad / 1000.0);
    }

    private double scoreStorage(Almacenamiento storage) {
        int capacidad = storage.getCapacidad() != null ? storage.getCapacidad() : 0;
        // Capamos la capacidad útil (2 TB): más allá no aporta rendimiento perceptible y evita que
        // un HDD enorme y barato gane la métrica solo por capacidad bruta.
        int capacidadUtil = Math.min(capacidad, CAP_STORAGE_GB);
        return (capacidadUtil * interfazMultiplier(storage)) / 1000.0;
    }

    // Multiplicador según la interfaz: la velocidad pesa mucho más que la capacidad. Con esto un
    // NVMe pequeño supera con holgura a un HDD grande (NVMe ×6, SSD SATA ×3, HDD ×1), que es lo
    // realista en un PC de escritorio: lo que importa es el tipo de disco, no los TB.
    private int interfazMultiplier(Almacenamiento storage) {
        String interfaz = storage.getInterfaz();
        if (interfaz != null) {
            String lower = interfaz.toLowerCase();
            if (lower.contains("m.2") || lower.contains("nvme") || lower.contains("pcie")) {
                return 6;
            }
        }
        return "SSD".equalsIgnoreCase(storage.getTipo()) ? 3 : 1;
    }
}

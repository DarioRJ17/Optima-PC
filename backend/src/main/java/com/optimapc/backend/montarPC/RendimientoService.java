package com.optimapc.backend.montarPC;

import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import com.optimapc.backend.modelo.Almacenamiento;
import com.optimapc.backend.modelo.ConfiguracionComponente;
import com.optimapc.backend.modelo.ConfiguracionPC;
import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.MemoriaRAM;
import com.optimapc.backend.modelo.Procesador;
import com.optimapc.backend.modelo.TarjetaGrafica;
import com.optimapc.backend.modelo.TipoUso;

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

    public double calcularScore(ConfiguracionPC config, TipoUso tipoUso) {
        double[] pesos = tipoUso != null ? PESOS.getOrDefault(tipoUso, PESOS_DEFAULT) : PESOS_DEFAULT;

        double cpuScore = 0, gpuScore = 0, ramScore = 0, storageScore = 0;

        for (ConfiguracionComponente cc : config.getComponentes()) {
            Componente proxy = cc.getComponente();
            if (proxy == null) continue;
            // Con LAZY + JOINED inheritance, Hibernate devuelve un proxy de la clase base (Componente),
            // no del subtipo real. El instanceof fallaría sin hacer unproxy primero.
            var componente = Hibernate.unproxy(proxy);
            int cantidad = cc.getCantidad() != null ? cc.getCantidad() : 1;

            if (componente instanceof Procesador cpu) {
                cpuScore = scoreCpu(cpu);
            } else if (componente instanceof TarjetaGrafica gpu) {
                gpuScore = scoreGpu(gpu);
            } else if (componente instanceof MemoriaRAM ram) {
                ramScore += scoreRam(ram) * cantidad;
            } else if (componente instanceof Almacenamiento storage) {
                storageScore += scoreStorage(storage) * cantidad;
            }
        }

        return pesos[0] * cpuScore
             + pesos[1] * gpuScore
             + pesos[2] * ramScore
             + pesos[3] * storageScore;
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

        double min = ratios.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = ratios.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double rango = max - min;

        for (int i = 0; i < configs.size(); i++) {
            double normalizado = rango > 0 ? ((ratios.get(i) - min) / rango) * 100.0 : 50.0;
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
        int boost = gpu.getFrecuenciaBoost() != null ? gpu.getFrecuenciaBoost() : 0;
        return (memoria * boost) / 1000.0;
    }

    private double scoreRam(MemoriaRAM ram) {
        int modulos = ram.getNumModulos() != null ? ram.getNumModulos() : 0;
        int gbPorModulo = ram.getGbPorModulo() != null ? ram.getGbPorModulo() : 0;
        int velocidad = ram.getVelocidad() != null ? ram.getVelocidad() : 0;
        return modulos * gbPorModulo * (velocidad / 1000.0);
    }

    private double scoreStorage(Almacenamiento storage) {
        int capacidad = storage.getCapacidad() != null ? storage.getCapacidad() : 0;
        return (capacidad * interfazMultiplier(storage)) / 1000.0;
    }

    // Asigna un multiplicador según la interfaz del almacenamiento para reflejar su impacto en el rendimiento
    private int interfazMultiplier(Almacenamiento storage) {
        String interfaz = storage.getInterfaz();
        if (interfaz != null) {
            String lower = interfaz.toLowerCase();
            if (lower.contains("m.2") || lower.contains("nvme") || lower.contains("pcie")) {
                return 3;
            }
        }
        return "SSD".equalsIgnoreCase(storage.getTipo()) ? 2 : 1;
    }
}

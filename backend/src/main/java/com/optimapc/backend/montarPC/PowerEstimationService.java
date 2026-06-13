package com.optimapc.backend.montarPC;

import java.util.List;

import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Caja;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.FuenteAlimentacion;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.PlacaBase;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.RefrigeradorCPU;
import com.optimapc.backend.domain.TarjetaGrafica;

public class PowerEstimationService {

    // Estimate consumption for a single component using: consumoWatts -> specific fields (tdp) -> heuristics
    public static int estimateConsumption(Componente c) {
        if (c == null) return 0;

        if (c instanceof FuenteAlimentacion) return 0; // PSUs are not consumers here

        if (c.getConsumoWatts() != null) return c.getConsumoWatts();

        if (c instanceof Procesador) {
            Integer tdp = ((Procesador) c).getTdp();
            if (tdp != null) return tdp;
            return 65; // default CPU
        }

        if (c instanceof TarjetaGrafica) {
            // no explicit field currently; assume typical mid-range if unknown
            return 150;
        }

        if (c instanceof MemoriaRAM) {
            Integer mods = ((MemoriaRAM) c).getNumModulos();
            if (mods != null) return mods * 3; // ~3W per module
            return 6;
        }

        if (c instanceof Almacenamiento) {
            String tipo = ((Almacenamiento) c).getTipo();
            if (tipo != null && tipo.toUpperCase().contains("HDD")) return 8;
            return 5; // SSD/NVMe
        }

        if (c instanceof PlacaBase) {
            return 40;
        }

        if (c instanceof RefrigeradorCPU) {
            return 10;
        }

        if (c instanceof Caja) {
            return 0;
        }

        // default fallback
        return 5;
    }

    // Estimate total consumption for selected components
    public static int estimateTotalWatts(List<Componente> selected) {
        if (selected == null || selected.isEmpty()) return 0;
        int sum = 0;
        for (Componente c : selected) {
            sum += estimateConsumption(c);
        }
        return sum;
    }

    // Check if PSU provides enough power given margin (e.g., 1.25)
    public static boolean isPowerSufficient(FuenteAlimentacion psu, List<Componente> selected, double margin) {
        if (psu == null || psu.getPotencia() == null) return false;
        int required = estimateTotalWatts(selected);
        double requiredWithMargin = required * margin;
        return psu.getPotencia() >= Math.ceil(requiredWithMargin);
    }
}

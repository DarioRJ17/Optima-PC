package com.optimapc.backend.montarPC;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.optimapc.backend.montarPC.dto.CompatibilityWarningDto;

import com.optimapc.backend.modelo.Almacenamiento;
import com.optimapc.backend.modelo.Caja;
import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.FuenteAlimentacion;
import com.optimapc.backend.modelo.MemoriaRAM;
import com.optimapc.backend.modelo.PlacaBase;
import com.optimapc.backend.modelo.Procesador;
import com.optimapc.backend.modelo.RefrigeradorCPU;
import com.optimapc.backend.modelo.TarjetaGrafica;

/**
 * Hook interface for component compatibility logic.
 * Implement this method to provide project-specific compatibility checks.
 */
public interface CompatibilityService {

    /**
     * Return true if the candidate component is compatible with the provided selected components.
     * Implementations should provide the actual rules. The default behavior here should
     * be implemented by the application developer.
     */
    public static boolean isCompatible(Componente candidate, List<Componente> selected) {
        List<String> componentesDelTipo = selected.stream()
                .map(componente -> MontarPCService.toTypeLabel(componente))
                .collect(Collectors.toList());

        if (candidate instanceof Procesador && componentesDelTipo.contains("placa-base")) {
            PlacaBase placaBaseSeleccionada = (PlacaBase) selected.stream()
                    .filter(c -> c instanceof PlacaBase)
                    .findFirst()
                    .orElse(null);
            return isCompatibleProcesadorVSPlacaBase((Procesador) candidate, placaBaseSeleccionada);
        }

        if (candidate instanceof PlacaBase && componentesDelTipo.contains("procesador")) {
            Procesador procesadorSeleccionado = (Procesador) selected.stream()
                    .filter(c -> c instanceof Procesador)
                    .findFirst()
                    .orElse(null);
            return isCompatiblePlacaBaseVSProcesador((PlacaBase) candidate, procesadorSeleccionado);
        }

        if (candidate instanceof MemoriaRAM  && componentesDelTipo.contains("placa-base")) {
            PlacaBase placaBaseSeleccionada = (PlacaBase) selected.stream()
                .filter(c -> c instanceof PlacaBase)
                .findFirst()
                .orElse(null);
            return isCompatibleRamVSPlacaBase((MemoriaRAM) candidate, placaBaseSeleccionada, selected);
        }

        if (candidate instanceof PlacaBase && componentesDelTipo.contains("memoria-ram")) {
            List<MemoriaRAM> ramsSeleccionadas = selected.stream()
                .filter(c -> c instanceof MemoriaRAM)
                .map(c -> (MemoriaRAM) c)
                .collect(Collectors.toList());
            return isCompatiblePlacaBaseVSRam((PlacaBase) candidate, ramsSeleccionadas);
        }

        if (candidate instanceof Caja && componentesDelTipo.contains("placa-base")) {
            PlacaBase placaBaseSeleccionada = (PlacaBase) selected.stream()
                    .filter(c -> c instanceof PlacaBase)
                    .findFirst()
                    .orElse(null);
            return isCompatibleCajaVSPlaca((Caja) candidate, placaBaseSeleccionada);
        }

        if (candidate instanceof PlacaBase && componentesDelTipo.contains("caja")) {
            Caja cajaSeleccionada = (Caja) selected.stream()
                    .filter(c -> c instanceof Caja)
                    .findFirst()
                    .orElse(null);
            return isCompatiblePlacaVSCaja((PlacaBase) candidate, cajaSeleccionada);
        }

        // Power checks:
        // If candidate is a PSU, ensure it can feed the selected components
        if (candidate instanceof FuenteAlimentacion) {
            FuenteAlimentacion fuente = (FuenteAlimentacion) candidate;
            // margin of safety 25%
            return PowerEstimationService.isPowerSufficient(fuente, selected, 1.25);
        }

        // If there is already a PSU selected, ensure it still suffices with the candidate added
        if (componentesDelTipo.contains("fuente-alimentacion")) {
            FuenteAlimentacion fuenteSeleccionada = (FuenteAlimentacion) selected.stream()
                    .filter(c -> c instanceof FuenteAlimentacion)
                    .findFirst()
                    .orElse(null);
            if (fuenteSeleccionada != null) {
                List<Componente> hipotetico = new ArrayList<>(selected);
                hipotetico.add(candidate);
                if (!PowerEstimationService.isPowerSufficient(fuenteSeleccionada, hipotetico, 1.25)) {
                    return false;
                }
            }
        }

        return true; // default to compatible if no specific rules apply

        // if (candidate instanceof Procesador) {
        //     return isCompatibleProcesador((Procesador) candidate, selected);
        // } else if (candidate instanceof PlacaBase) {
        //     return isCompatiblePlacaBase((PlacaBase) candidate, selected);
        // } else if (candidate instanceof MemoriaRAM) {
        //     return isCompatibleMemoriaRAM((MemoriaRAM) candidate, selected);
        // } else if (candidate instanceof TarjetaGrafica) {
        //     return isCompatibleTarjetaGrafica((TarjetaGrafica) candidate, selected);
        // } else if (candidate instanceof Almacenamiento) {
        //     return isCompatibleAlmacenamiento((Almacenamiento) candidate, selected);
        // } else if (candidate instanceof FuenteAlimentacion) {
        //     return isCompatibleFuenteAlimentacion((FuenteAlimentacion) candidate, selected);
        // } else if (candidate instanceof Caja) {
        //     return isCompatibleCaja((Caja) candidate, selected);
        // } else if (candidate instanceof RefrigeradorCPU) {
        //     return isCompatibleRefrigeradorCPU((RefrigeradorCPU) candidate, selected);
        // }
    }

    private static boolean isCompatibleProcesadorVSPlacaBase(Procesador candidate, PlacaBase placaBaseSeleccionada) {
        if (placaBaseSeleccionada == null || candidate.getSocket() == null || placaBaseSeleccionada.getSocket() == null)
            return false;
        if (candidate.getSocket().equals(placaBaseSeleccionada.getSocket()))
            return true;

        return false;
    }

    private static boolean isCompatiblePlacaBaseVSProcesador(PlacaBase candidate, Procesador procesadorSeleccionado) {
        if (procesadorSeleccionado == null || candidate.getSocket() == null || procesadorSeleccionado.getSocket() == null)
            return false;
        if (candidate.getSocket().equals(procesadorSeleccionado.getSocket()))
            return true;

        return false;
    }

    private static boolean isCompatibleRamVSPlacaBase(MemoriaRAM candidate, PlacaBase placaBaseSeleccionada, List<Componente> selected) {
        if (placaBaseSeleccionada == null)
            return false;

        String placaDDR = placaBaseSeleccionada.getTipoDDR();
        String ramDDR = candidate.getTipoDDR();
        if (placaDDR == null || ramDDR == null)
            return false;
        if (!soportaDDR(placaDDR, ramDDR))
            return false; // different DDR generation

        List<MemoriaRAM> memoriasSeleccionadas = extraerMemoriasRAM(selected);
        int totalModules = totalModulosRAM(memoriasSeleccionadas) + valorOZero(candidate.getNumModulos());
        int totalGB = totalGBRAM(memoriasSeleccionadas) + totalGBRAM(candidate);

        return cumpleLimitesRAM(placaBaseSeleccionada.getRanurasMemoria(), placaBaseSeleccionada.getMemoriaMaxima(), totalModules, totalGB);
    }

    private static boolean isCompatiblePlacaBaseVSRam(PlacaBase candidate, List<MemoriaRAM> ramsSeleccionadas) {
        if (candidate == null)
            return false;

        String placaDDR = candidate.getTipoDDR();
        if (placaDDR == null)
            return false;

        for (MemoriaRAM ram : ramsSeleccionadas) {
            String ramDDR = ram.getTipoDDR();
            if (ramDDR == null || !soportaDDR(placaDDR, ramDDR))
                return false; // DDR mismatch
        }

        int totalModules = totalModulosRAM(ramsSeleccionadas);
        int totalGB = totalGBRAM(ramsSeleccionadas);

        return cumpleLimitesRAM(candidate.getRanurasMemoria(), candidate.getMemoriaMaxima(), totalModules, totalGB);
    }

    public static boolean isCompatibleCajaVSPlaca(Caja candidate, PlacaBase placaSeleccionada) {
        if (candidate.getTipo() == null || placaSeleccionada.getFactorForma() == null) {
            return false;
        }

        String caja = normalizar(candidate.getTipo());
        String placa = normalizar(placaSeleccionada.getFactorForma());

        Set<String> soportados = formatosSoportadosPorCaja(caja);

        return soportados.contains(placa);
    }

    public static boolean isCompatiblePlacaVSCaja(PlacaBase candidate, Caja cajaSeleccionada) {
        if (cajaSeleccionada.getTipo() == null || candidate.getFactorForma() == null) {
            return false;
        }

        String caja = normalizar(cajaSeleccionada.getTipo());
        String placa = normalizar(candidate.getFactorForma());

        Set<String> soportados = formatosSoportadosPorCaja(caja);

        return soportados.contains(placa);
    }

    public static List<CompatibilityWarningDto> buildWarnings(Componente candidate, List<Componente> selected) {
        if (candidate instanceof MemoriaRAM memoriaRAM) {
            return buildWarningsForRam(memoriaRAM, extraerMemoriasRAM(selected));
        }

        return List.of();
    }

    private static List<CompatibilityWarningDto> buildWarningsForRam(MemoriaRAM candidate, List<MemoriaRAM> selectedRams) {
        if (selectedRams.isEmpty()) {
            return List.of();
        }

        List<CompatibilityWarningDto> warnings = new ArrayList<>();
        if (tieneFrecuenciaDistinta(candidate, selectedRams)) {
            warnings.add(new CompatibilityWarningDto(
                    "ram_frequency_mismatch",
                    "Mezclar módulos con distinta frecuencia es compatible, pero el conjunto funcionará a la velocidad del módulo más lento."));
        }

        if (tieneLatenciaDistinta(candidate, selectedRams)) {
            warnings.add(new CompatibilityWarningDto(
                    "ram_latency_mismatch",
                    "Mezclar módulos con distinta latencia CAS es compatible, pero el ajuste final quedará condicionado por el módulo más conservador."));
        }

        if (tieneCapacidadDistinta(candidate, selectedRams)) {
            warnings.add(new CompatibilityWarningDto(
                    "ram_capacity_mismatch",
                    "Mezclar módulos con distinta capacidad es compatible, pero no es la configuración más homogénea."));
        }

        return warnings;
    }

    private static boolean tieneFrecuenciaDistinta(MemoriaRAM candidate, List<MemoriaRAM> selectedRams) {
        return hasDifferentValue(candidate.getVelocidad(), selectedRams.stream().map(MemoriaRAM::getVelocidad).toList());
    }

    private static boolean tieneLatenciaDistinta(MemoriaRAM candidate, List<MemoriaRAM> selectedRams) {
        return hasDifferentValue(candidate.getLatenciaCAS(), selectedRams.stream().map(MemoriaRAM::getLatenciaCAS).toList());
    }

    private static boolean tieneCapacidadDistinta(MemoriaRAM candidate, List<MemoriaRAM> selectedRams) {
        return hasDifferentValue(candidate.getGbPorModulo(), selectedRams.stream().map(MemoriaRAM::getGbPorModulo).toList());
    }

    private static boolean hasDifferentValue(Integer candidateValue, List<Integer> values) {
        if (candidateValue == null) {
            return false;
        }

        return values.stream()
                .filter(Objects::nonNull)
                .anyMatch(value -> !candidateValue.equals(value));
    }

    private static boolean soportaDDR(String valorPlaca, String valorRam) {
        if (valorPlaca == null || valorRam == null) {
            return false;
        }

        String placaNormalizada = valorPlaca.trim().toUpperCase(Locale.ROOT);
        String ramNormalizada = valorRam.trim().toUpperCase(Locale.ROOT);

        return placaNormalizada.contains(ramNormalizada);
    }

    private static List<MemoriaRAM> extraerMemoriasRAM(List<Componente> selected) {
        return selected.stream()
                .filter(c -> c instanceof MemoriaRAM)
                .map(c -> (MemoriaRAM) c)
                .collect(Collectors.toList());
    }

    private static int totalModulosRAM(List<MemoriaRAM> memorias) {
        return memorias.stream()
                .mapToInt(CompatibilityService::modulosDeRAM)
                .sum();
    }

    private static int totalGBRAM(List<MemoriaRAM> memorias) {
        return memorias.stream()
                .mapToInt(CompatibilityService::gbTotalesDeRAM)
                .sum();
    }

    private static int totalGBRAM(MemoriaRAM memoria) {
        return gbTotalesDeRAM(memoria);
    }

    private static int modulosDeRAM(MemoriaRAM memoria) {
        return valorOZero(memoria.getNumModulos());
    }

    private static int gbTotalesDeRAM(MemoriaRAM memoria) {
        Integer numModulos = memoria.getNumModulos();
        Integer gbPorModulo = memoria.getGbPorModulo();
        if (numModulos == null || gbPorModulo == null) {
            return 0;
        }

        return numModulos * gbPorModulo;
    }

    private static int valorOZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static boolean cumpleLimitesRAM(Integer ranurasMaximas, Integer memoriaMaxima, int totalModulos, int totalGB) {
        if (ranurasMaximas != null && totalModulos > ranurasMaximas) {
            return false;
        }

        if (memoriaMaxima != null && totalGB > memoriaMaxima) {
            return false;
        }

        return true;
    }

    private static Set<String> formatosSoportadosPorCaja(String tipoCajaNormalizado) {
        if (tipoCajaNormalizado.contains("ATX FULL TOWER")) {
            return Set.of("ATX", "MICRO ATX", "MINI ITX", "MINI DTX", "THIN MINI ITX", "EATX");
        }

        if (tipoCajaNormalizado.contains("ATX")) {
            return Set.of("ATX", "MICRO ATX", "MINI ITX", "MINI DTX", "THIN MINI ITX");
        }

        if (tipoCajaNormalizado.contains("MICROATX")) {
            return Set.of("MICRO ATX", "MINI ITX", "MINI DTX", "THIN MINI ITX");
        }

        if (tipoCajaNormalizado.contains("MINI ITX") || tipoCajaNormalizado.contains("HTPC")) {
            return Set.of("MINI ITX", "THIN MINI ITX");
        }

        return Set.of();
    }

    private static String normalizar(String valor) {
        String limpio = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");

        limpio = limpio.replace("MICROATX", "MICRO ATX");
        limpio = limpio.replace("MINI-ITX", "MINI ITX");
        limpio = limpio.replace("MINI- DTX", "MINI DTX");
        limpio = limpio.replace("E-ATX", "EATX");
        limpio = limpio.replace("XL-ATX", "XL ATX");

        return limpio;
    }

    

    // public boolean isCompatibleTarjetaGrafica(TarjetaGrafica candidate, List<Componente> selected) {
    //     return true;
    // }

    // public boolean isCompatibleAlmacenamiento(Almacenamiento candidate, List<Componente> selected) {
    //     return true;
    // }

    // public boolean isCompatibleFuenteAlimentacion(FuenteAlimentacion candidate, List<Componente> selected) {
    //     return true;
    // }

    // public boolean isCompatibleCaja(Caja candidate, List<Componente> selected) {
    //     return true;
    // }

    // public boolean isCompatibleRefrigeradorCPU(RefrigeradorCPU candidate, List<Componente> selected) {
    //     return true;
    // }
}

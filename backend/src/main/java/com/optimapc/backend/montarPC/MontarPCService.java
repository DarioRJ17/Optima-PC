package com.optimapc.backend.montarPC;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.catalogo.ComponenteDto;
import com.optimapc.backend.montarPC.dto.CompatibleComponenteDto;
import com.optimapc.backend.montarPC.dto.ComponenteDetalleDto;
import com.optimapc.backend.montarPC.dto.CompatibilityWarningDto;
import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.Almacenamiento;
import com.optimapc.backend.modelo.Caja;
import com.optimapc.backend.modelo.ConfiguracionComponente;
import com.optimapc.backend.modelo.ConfiguracionPC;
import com.optimapc.backend.modelo.FuenteAlimentacion;
import com.optimapc.backend.modelo.MemoriaRAM;
import com.optimapc.backend.modelo.PlacaBase;
import com.optimapc.backend.modelo.Procesador;
import com.optimapc.backend.modelo.RefrigeradorCPU;
import com.optimapc.backend.modelo.TarjetaGrafica;
import com.optimapc.backend.pedido.ConfiguracionPCRepository;

@Service
public class MontarPCService {

    private final ComponenteRepository componenteRepository;
    private final RendimientoService rendimientoService;
    private final ConfiguracionPCRepository configuracionPCRepository;

    public MontarPCService(ComponenteRepository componenteRepository, RendimientoService rendimientoService,
            ConfiguracionPCRepository configuracionPCRepository) {
        this.componenteRepository = componenteRepository;
        this.rendimientoService = rendimientoService;
        this.configuracionPCRepository = configuracionPCRepository;
    }

    public List<ComponenteDto> getAllComponents() {
        return componenteRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ComponenteDto> getCompatibleComponents(List<Long> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return getAllComponents();
        }

        List<Componente> selected = componenteRepository.findAllById(selectedIds);
        List<Componente> all = componenteRepository.findAll();

        return all.stream()
                .filter(candidate -> isAlreadySelected(candidate, selected) || CompatibilityService.isCompatible(candidate, selected))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CompatibleComponenteDto> getCompatibleComponentsWithWarnings(List<Long> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return componenteRepository.findAll().stream()
                    .map(this::mapToCompatibleDto)
                    .collect(Collectors.toList());
        }

        List<Componente> selected = componenteRepository.findAllById(selectedIds);
        List<Componente> all = componenteRepository.findAll();

        return all.stream()
                .filter(candidate -> isAlreadySelected(candidate, selected) || CompatibilityService.isCompatible(candidate, selected))
                .map(candidate -> mapToCompatibleDto(candidate, selected))
                .collect(Collectors.toList());
    }

    public List<ComponenteDto> getComponentsByType(String tipo, List<Long> selectedIds) {
        List<Componente> componentesDelTipo = componenteRepository.findAll().stream()
                .filter(componente -> matchesType(componente, tipo))
                .collect(Collectors.toList());

        if (selectedIds == null || selectedIds.isEmpty()) {
            return componentesDelTipo.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        List<Componente> selected = componenteRepository.findAllById(selectedIds);

        return componentesDelTipo.stream()
                .filter(candidate -> isAlreadySelected(candidate, selected) || CompatibilityService.isCompatible(candidate, selected))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<CompatibleComponenteDto> getComponentsByTypeWithWarnings(String tipo, List<Long> selectedIds) {
        List<Componente> componentesDelTipo = componenteRepository.findAll().stream()
                .filter(componente -> matchesType(componente, tipo))
                .collect(Collectors.toList());

        if (selectedIds == null || selectedIds.isEmpty()) {
            return componentesDelTipo.stream()
                    .map(this::mapToCompatibleDto)
                    .collect(Collectors.toList());
        }

        List<Componente> selected = componenteRepository.findAllById(selectedIds);

        return componentesDelTipo.stream()
                .filter(candidate -> isAlreadySelected(candidate, selected) || CompatibilityService.isCompatible(candidate, selected))
                .map(candidate -> mapToCompatibleDto(candidate, selected))
                .collect(Collectors.toList());
    }

    public List<String> getAvailableTypes() {
        return List.of(
                "procesador",
                "placa-base",
                "memoria-ram",
                "tarjeta-grafica",
                "almacenamiento",
                "fuente-alimentacion",
                "caja",
                "refrigerador-cpu");
    }

    private ComponenteDto mapToDto(Componente c) {
        String tipo = toTypeLabel(c);
        String nombre = c instanceof TarjetaGrafica tg ? tg.getChipset() : c.getNombre();
        String especificacion = buildSpecification(c);
        return new ComponenteDto(c.getId(), tipo, nombre, especificacion, c.getPrecio(), 1);
    }

    private CompatibleComponenteDto mapToCompatibleDto(Componente c) {
        return mapToCompatibleDto(c, List.of());
    }

    private CompatibleComponenteDto mapToCompatibleDto(Componente c, List<Componente> selected) {
        String tipo = toTypeLabel(c);
        String nombre = c instanceof TarjetaGrafica tg ? tg.getChipset() : c.getNombre();
        String especificacion = buildSpecification(c);
        List<CompatibilityWarningDto> warnings = CompatibilityService.buildWarnings(c, selected);
        Map<String, Object> propiedades = buildComponenteDetalles(c);
        return new CompatibleComponenteDto(c.getId(), tipo, nombre, especificacion, c.getPrecio(), 1, warnings, propiedades);
    }

    private boolean isAlreadySelected(Componente candidate, List<Componente> selected) {
        return selected.stream().anyMatch(s -> s.getId().equals(candidate.getId()));
    }

    private boolean matchesType(Componente componente, String tipo) {
        String normalizedType = normalizeType(tipo);

        return switch (normalizedType) {
            case "procesador" -> componente instanceof Procesador;
            case "placa-base" -> componente instanceof PlacaBase;
            case "memoria-ram" -> componente instanceof MemoriaRAM;
            case "tarjeta-grafica" -> componente instanceof TarjetaGrafica;
            case "almacenamiento" -> componente instanceof Almacenamiento;
            case "fuente-alimentacion" -> componente instanceof FuenteAlimentacion;
            case "caja" -> componente instanceof Caja;
            case "refrigerador-cpu" -> componente instanceof RefrigeradorCPU;
            default -> false;
        };
    }

    public static String toTypeLabel(Componente componente) {
        if (componente instanceof Procesador) {
            return "procesador";
        }
        if (componente instanceof PlacaBase) {
            return "placa-base";
        }
        if (componente instanceof MemoriaRAM) {
            return "memoria-ram";
        }
        if (componente instanceof TarjetaGrafica) {
            return "tarjeta-grafica";
        }
        if (componente instanceof Almacenamiento) {
            return "almacenamiento";
        }
        if (componente instanceof FuenteAlimentacion) {
            return "fuente-alimentacion";
        }
        if (componente instanceof Caja) {
            return "caja";
        }
        if (componente instanceof RefrigeradorCPU) {
            return "refrigerador-cpu";
        }
        return componente.getClass().getSimpleName().toLowerCase();
    }

    private String buildSpecification(Componente componente) {
        String res = "";
        if (componente instanceof TarjetaGrafica tg) {
            res = tg.getMemoria() + " GB | " + tg.getLongitud() + " mm | " + formatFrecuencia(tg.getFrecuenciaBase(), tg.getFrecuenciaBoost());
            if (tg.getConsumoWatts() != null) res += " | " + tg.getConsumoWatts() + " W";
        } else if (componente instanceof Procesador p) {
            res = p.getSocket() + " | " + p.getNucleos() + " núcleos | " + formatFrecuencia(p.getFrecuenciaBase(), p.getFrecuenciaBoost());
            if (p.getConsumoWatts() != null) res += " | " + p.getConsumoWatts() + " W";
            else if (p.getTdp() != null) res += " | " + p.getTdp() + " W";
        } else if (componente instanceof MemoriaRAM ram) {
            res = ram.getTipoDDR() + " | " + ram.getVelocidad() + " MHz | " + ram.getNumModulos() + "x" + ram.getGbPorModulo() + " GB | CL" + ram.getLatenciaCAS();
            if (ram.getConsumoWatts() != null) res += " | " + ram.getConsumoWatts() + " W";
        } else if (componente instanceof Almacenamiento a) {
            res = a.getCapacidad() + " GB " + a.getTipo() + " | Interfaz: " + a.getInterfaz() + " | FF: " + a.getFactorForma();
            if (a.getConsumoWatts() != null) res += " | " + a.getConsumoWatts() + " W";
        } else if (componente instanceof PlacaBase pb) {
            res = pb.getSocket() + " | " + pb.getTipoDDR() + " | FF: " + pb.getFactorForma() + " | " + pb.getMemoriaMaxima() + " GB en " + pb.getRanurasMemoria() + " ranuras";
            if (pb.getConsumoWatts() != null) res += " | " + pb.getConsumoWatts() + " W";
        } else if (componente instanceof FuenteAlimentacion fa) {
            res = fa.getPotencia() + " W | " + fa.getTipo() + " | Modular: " + fa.getModular();
            if (fa.getConsumoWatts() != null) res += " | " + fa.getConsumoWatts() + " W";
        } else if (componente instanceof Caja c) {
            res = c.getTipo() + " | Panel lateral: " + c.getPanelLateral();
            if (c.getConsumoWatts() != null) res += " | " + c.getConsumoWatts() + " W";
        } else if (componente instanceof RefrigeradorCPU r) {
            res = r.getRpm() + " RPM | " + r.getNivelRuido() + " dBA";
            if (r.getConsumoWatts() != null) res += " | " + r.getConsumoWatts() + " W";
        }
        return res;
    }

    private String formatFrecuencia(Number base, Number boost) {
        if (boost != null) {
            return base + "-" + boost + " MHz";
        }
        return base + " MHz";
    }

    public Optional<ComponenteDetalleDto> getComponenteDetalle(Long id) {
        return componenteRepository.findById(id).map(this::mapToComponenteDetalleDto);
    }

    private ComponenteDetalleDto mapToComponenteDetalleDto(Componente c) {
        String tipo = toTypeLabel(c);
        String nombre = c instanceof TarjetaGrafica tg ? tg.getChipset() : c.getNombre();
        String especificacion = buildSpecification(c);
        Map<String, Object> detalles = buildComponenteDetalles(c);

        return new ComponenteDetalleDto(
                c.getId(),
                tipo,
                nombre,
                especificacion,
                c.getPrecio(),
                1,
                c.getConsumoWatts(),
                detalles);
    }

    private Map<String, Object> buildComponenteDetalles(Componente componente) {
        Map<String, Object> detalles = new HashMap<>();
        String tipo = toTypeLabel(componente);

        detalles.put("nombre", componente.getNombre());
        detalles.put("precio", componente.getPrecio());
        if (componente.getConsumoWatts() != null) {
            detalles.put("consumoWatts", componente.getConsumoWatts());
        }

        switch (tipo) {
            case "procesador" -> {
                Procesador procesador = (Procesador) componente;
                detalles.put("socket", procesador.getSocket());
                detalles.put("microarquitectura", procesador.getMicroarquitectura());
                detalles.put("nucleos", procesador.getNucleos());
                detalles.put("frecuenciaBase", procesador.getFrecuenciaBase());
                detalles.put("frecuenciaBoost", procesador.getFrecuenciaBoost());
                detalles.put("tdp", procesador.getTdp());
                detalles.put("graficaIntegrada", procesador.getGraficaIntegrada());
            }
            case "placa-base" -> {
                PlacaBase placaBase = (PlacaBase) componente;
                detalles.put("socket", placaBase.getSocket());
                detalles.put("factorForma", placaBase.getFactorForma());
                detalles.put("tipoDDR", placaBase.getTipoDDR());
                detalles.put("memoriaMaxima", placaBase.getMemoriaMaxima());
                detalles.put("ranurasMemoria", placaBase.getRanurasMemoria());
                detalles.put("color", placaBase.getColor());
            }
            case "memoria-ram" -> {
                MemoriaRAM memoriaRAM = (MemoriaRAM) componente;
                detalles.put("tipoDDR", memoriaRAM.getTipoDDR());
                detalles.put("velocidad", memoriaRAM.getVelocidad());
                detalles.put("gbPorModulo", memoriaRAM.getGbPorModulo());
                detalles.put("numModulos", memoriaRAM.getNumModulos());
                detalles.put("latenciaCAS", memoriaRAM.getLatenciaCAS());
                detalles.put("color", memoriaRAM.getColor());
                detalles.put("totalGB", memoriaRAM.getGbPorModulo() * memoriaRAM.getNumModulos());
            }
            case "tarjeta-grafica" -> {
                TarjetaGrafica tarjetaGrafica = (TarjetaGrafica) componente;
                detalles.put("modelo", tarjetaGrafica.getNombre());
                detalles.put("fabricante", inferirFabricante(tarjetaGrafica.getChipset()));
                detalles.put("memoria", tarjetaGrafica.getMemoria());
                detalles.put("frecuenciaBase", tarjetaGrafica.getFrecuenciaBase());
                detalles.put("frecuenciaBoost", tarjetaGrafica.getFrecuenciaBoost());
                detalles.put("color", tarjetaGrafica.getColor());
                detalles.put("longitud", tarjetaGrafica.getLongitud());
            }
            case "almacenamiento" -> {
                Almacenamiento almacenamiento = (Almacenamiento) componente;
                detalles.put("tipo", almacenamiento.getTipo());
                detalles.put("capacidad", almacenamiento.getCapacidad());
                detalles.put("interfaz", almacenamiento.getInterfaz());
                detalles.put("factorForma", almacenamiento.getFactorForma());
            }
            case "fuente-alimentacion" -> {
                FuenteAlimentacion fuente = (FuenteAlimentacion) componente;
                detalles.put("potencia", fuente.getPotencia());
                detalles.put("eficiencia", fuente.getEficiencia());
                detalles.put("tipo", fuente.getTipo());
                detalles.put("modular", fuente.getModular());
                detalles.put("color", fuente.getColor());
            }
            case "caja" -> {
                Caja caja = (Caja) componente;
                detalles.put("tipo", caja.getTipo());
                detalles.put("color", caja.getColor());
                detalles.put("panelLateral", caja.getPanelLateral());
            }
            case "refrigerador-cpu" -> {
                RefrigeradorCPU refrigerador = (RefrigeradorCPU) componente;
                detalles.put("rpm", refrigerador.getRpm());
                detalles.put("nivelRuido", refrigerador.getNivelRuido());
                detalles.put("tamano", refrigerador.getTamano());
                detalles.put("color", refrigerador.getColor());
            }
        }

        return detalles;
    }

    public ConsumoDto calcularConsumo(List<Long> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return new ConsumoDto(0, 0, null, null, true);
        }
        List<Componente> componentes = componenteRepository.findAllById(selectedIds);
        FuenteAlimentacion psu = componentes.stream()
                .filter(c -> c instanceof FuenteAlimentacion)
                .map(c -> (FuenteAlimentacion) c)
                .findFirst()
                .orElse(null);
        int consumo = PowerEstimationService.estimateTotalWatts(componentes);
        int recomendado = (int) Math.ceil(consumo * 1.25);
        Integer potenciaPSU = psu != null ? psu.getPotencia() : null;
        boolean suficiente = psu == null || PowerEstimationService.isPowerSufficient(psu, componentes, 1.25);
        Integer disponible = potenciaPSU != null ? (int) Math.floor(potenciaPSU * 0.8) - consumo : null;
        return new ConsumoDto(consumo, recomendado, potenciaPSU, disponible, suficiente);
    }

    public EquilibrioResult calcularEquilibrio(List<Long> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return new EquilibrioResult(0.0, List.of());
        }
        List<Componente> componentes = componenteRepository.findAllById(selectedIds);
        ConfiguracionPC config = new ConfiguracionPC();
        for (Componente c : componentes) {
            ConfiguracionComponente cc = new ConfiguracionComponente();
            cc.setComponente(c);
            cc.setCantidad(1);
            config.agregarComponente(cc);
        }
        return rendimientoService.calcularEquilibrio(config, null);
    }

    private String inferirFabricante(String chipset) {
        if (chipset == null) return "Otro";
        String lower = chipset.toLowerCase();
        if (lower.contains("rtx") || lower.contains("gtx") || lower.contains("quadro")
                || lower.contains("geforce") || lower.contains("titan")) return "NVIDIA";
        if (lower.contains(" rx ") || lower.contains("radeon") || lower.contains("vega")
                || lower.contains("fury") || lower.contains("firepro")) return "AMD";
        if (lower.contains("arc") || lower.contains("iris") || lower.contains("uhd")
                || lower.contains("hd graphics") || lower.contains("xe")) return "Intel";
        return "Otro";
    }

    private String normalizeType(String tipo) {
        return tipo == null ? "" : tipo.trim().toLowerCase();
    }

    @Transactional
    public ConfiguracionGuardadaDto guardarConfiguracion(List<Long> componenteIds) {
        List<Componente> componentes = componenteRepository.findAllById(componenteIds);
        if (componentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se encontraron los componentes indicados");
        }
        ConfiguracionPC config = new ConfiguracionPC();
        for (Componente c : componentes) {
            ConfiguracionComponente cc = new ConfiguracionComponente();
            cc.setCategoria(toTypeLabel(c));
            cc.setCantidad(1);
            cc.setComponente(c);
            config.agregarComponente(cc);
        }
        ConfiguracionPC saved = configuracionPCRepository.save(config);
        return new ConfiguracionGuardadaDto(saved.getId(), saved.getPrecio());
    }
}

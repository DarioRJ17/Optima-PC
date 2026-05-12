package com.optimapc.backend.montarPC;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.optimapc.backend.catalogo.ComponenteDto;
import com.optimapc.backend.montarPC.dto.CompatibleComponenteDto;
import com.optimapc.backend.montarPC.dto.CompatibilityWarningDto;
import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.Almacenamiento;
import com.optimapc.backend.modelo.Caja;
import com.optimapc.backend.modelo.FuenteAlimentacion;
import com.optimapc.backend.modelo.MemoriaRAM;
import com.optimapc.backend.modelo.PlacaBase;
import com.optimapc.backend.modelo.Procesador;
import com.optimapc.backend.modelo.RefrigeradorCPU;
import com.optimapc.backend.modelo.TarjetaGrafica;

@Service
public class MontarPCService {

    private final ComponenteRepository componenteRepository;

    public MontarPCService(ComponenteRepository componenteRepository) {
        this.componenteRepository = componenteRepository;
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
        String especificacion = buildSpecification(c);
        return new ComponenteDto(c.getId(), tipo, c.getNombre(), especificacion, c.getPrecio(), 1);
    }

    private CompatibleComponenteDto mapToCompatibleDto(Componente c) {
        return mapToCompatibleDto(c, List.of());
    }

    private CompatibleComponenteDto mapToCompatibleDto(Componente c, List<Componente> selected) {
        String tipo = toTypeLabel(c);
        String especificacion = buildSpecification(c);
        List<CompatibilityWarningDto> warnings = CompatibilityService.buildWarnings(c, selected);
        return new CompatibleComponenteDto(c.getId(), tipo, c.getNombre(), especificacion, c.getPrecio(), 1, warnings);
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
        return switch (toTypeLabel(componente)) {
            case "procesador" -> {
                Procesador procesador = (Procesador) componente;
                yield procesador.getMicroarquitectura();
            }
            case "placa-base" -> {
                PlacaBase placaBase = (PlacaBase) componente;
                yield placaBase.getSocket() + " | " + placaBase.getTipoDDR();
            }
            case "memoria-ram" -> {
                MemoriaRAM memoriaRAM = (MemoriaRAM) componente;
                yield memoriaRAM.getTipoDDR() + " | " + memoriaRAM.getVelocidad();
            }
            case "tarjeta-grafica" -> componente.getNombre();
            case "almacenamiento" -> {
                Almacenamiento almacenamiento = (Almacenamiento) componente;
                yield almacenamiento.getTipo() + " | " + almacenamiento.getInterfaz();
            }
            case "fuente-alimentacion" -> {
                FuenteAlimentacion fuente = (FuenteAlimentacion) componente;
                yield fuente.getPotencia() + "W | " + fuente.getEficiencia();
            }
            case "caja" -> componente.getNombre();
            case "refrigerador-cpu" -> componente.getNombre();
            default -> componente.getNombre();
        };
    }

    private String normalizeType(String tipo) {
        return tipo == null ? "" : tipo.trim().toLowerCase();
    }
}

package com.optimapc.backend.montarPC;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.optimapc.backend.catalogo.ComponenteDto;
import com.optimapc.backend.montarPC.dto.CompatibleComponenteDto;
import com.optimapc.backend.montarPC.dto.ComponenteDetalleDto;
import com.optimapc.backend.montarPC.EquilibrioResult;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configuracion-pc")
public class ConfiguracionPCController {

    private final MontarPCService montarPCService;

    public ConfiguracionPCController(MontarPCService montarPCService) {
        this.montarPCService = montarPCService;
    }

    @GetMapping("/components")
    public List<ComponenteDto> getAllComponents() {
        return montarPCService.getAllComponents();
    }

    @GetMapping("/components/compatible")
    public List<ComponenteDto> getCompatibleComponents(@RequestParam(required = false) List<Long> selectedIds) {
        return montarPCService.getCompatibleComponents(selectedIds);
    }

    @GetMapping("/components/compatible-with-warnings")
    public List<CompatibleComponenteDto> getCompatibleComponentsWithWarnings(
            @RequestParam(required = false) List<Long> selectedIds) {
        return montarPCService.getCompatibleComponentsWithWarnings(selectedIds);
    }

    @GetMapping("/components/{id}/detail")
    public ResponseEntity<ComponenteDetalleDto> getComponenteDetalle(@PathVariable Long id) {
        return montarPCService.getComponenteDetalle(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/equilibrio")
    public EquilibrioResult getEquilibrio(@RequestParam(required = false) List<Long> selectedIds) {
        return montarPCService.calcularEquilibrio(selectedIds);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguracionGuardadaDto guardarConfiguracion(@RequestBody @Valid GuardarConfiguracionRequest request) {
        return montarPCService.guardarConfiguracion(request.componenteIds());
    }
}

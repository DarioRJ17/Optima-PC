package com.optimapc.backend.montarPC;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.optimapc.backend.catalogo.ComponenteDto;
import com.optimapc.backend.montarPC.dto.CompatibleComponenteDto;

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
}

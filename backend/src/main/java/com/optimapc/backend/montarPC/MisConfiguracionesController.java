package com.optimapc.backend.montarPC;

import com.optimapc.backend.montarPC.dto.MiConfiguracionDto;
import com.optimapc.backend.montarPC.dto.GuardarConfiguracionNombradaRequest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/mis-configuraciones")
public class MisConfiguracionesController {

    private final MisConfiguracionesService misConfiguracionesService;

    public MisConfiguracionesController(MisConfiguracionesService misConfiguracionesService) {
        this.misConfiguracionesService = misConfiguracionesService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MiConfiguracionDto guardar(Authentication authentication,
            @Valid @RequestBody GuardarConfiguracionNombradaRequest request) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return misConfiguracionesService.guardar(usuarioId, request.componenteIds(), request.nombre());
    }

    @GetMapping
    public List<MiConfiguracionDto> listar(Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return misConfiguracionesService.listar(usuarioId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(Authentication authentication, @PathVariable Long id) {
        Long usuarioId = (Long) authentication.getPrincipal();
        misConfiguracionesService.eliminar(id, usuarioId);
    }
}

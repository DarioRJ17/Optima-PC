package com.optimapc.backend.usuario;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.optimapc.backend.auth.dto.MessageResponse;
import com.optimapc.backend.usuario.dto.EncuestaInicialRequest;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/perfil-usuario")
public class PerfilUsuarioController {

    private final PerfilUsuarioService perfilUsuarioService;

    public PerfilUsuarioController(PerfilUsuarioService perfilUsuarioService) {
        this.perfilUsuarioService = perfilUsuarioService;
    }

    @PostMapping("/encuesta-inicial")
    public ResponseEntity<MessageResponse> guardarEncuestaInicial(
            Authentication authentication,
            @Valid @RequestBody EncuestaInicialRequest request) {
        Long usuarioId = (Long) authentication.getPrincipal();
        perfilUsuarioService.guardarEncuestaInicial(usuarioId, request);
        return ResponseEntity.ok(new MessageResponse("Encuesta guardada correctamente"));
    }
}
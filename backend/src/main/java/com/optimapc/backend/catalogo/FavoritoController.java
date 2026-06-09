package com.optimapc.backend.catalogo;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogo")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @PostMapping("/premontados/{id}/favoritos")
    public ResponseEntity<FavoritoDto> añadir(Authentication authentication, @PathVariable Long id) {
        Long usuarioId = (Long) authentication.getPrincipal();
        FavoritoDto dto = favoritoService.añadir(usuarioId, id);
        return ResponseEntity.status(201).body(dto);
    }

    @DeleteMapping("/premontados/{id}/favoritos")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable Long id) {
        Long usuarioId = (Long) authentication.getPrincipal();
        favoritoService.eliminar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/favoritos")
    public ResponseEntity<List<FavoritoDto>> listar(Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(favoritoService.listarDeUsuario(usuarioId));
    }

    @GetMapping("/premontados/{id}/favoritos/estado")
    public ResponseEntity<Boolean> estado(Authentication authentication, @PathVariable Long id) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(favoritoService.esFavorito(usuarioId, id));
    }
}

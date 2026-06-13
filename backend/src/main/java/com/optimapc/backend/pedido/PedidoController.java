package com.optimapc.backend.pedido;

import com.optimapc.backend.pedido.dto.PedidoDto;
import com.optimapc.backend.pedido.dto.CrearPedidoRequest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PedidoDto> crear(
            Authentication authentication,
            @Valid @RequestBody CrearPedidoRequest request) {
        Long usuarioId = (Long) authentication.getPrincipal();
        PedidoDto pedido = pedidoService.crear(usuarioId, request);
        return ResponseEntity.status(201).body(pedido);
    }

    @GetMapping
    public ResponseEntity<List<PedidoDto>> listar(Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(pedidoService.listarDeUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDto> obtener(
            Authentication authentication,
            @PathVariable Long id) {
        Long usuarioId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(pedidoService.obtenerPorId(usuarioId, id));
    }
}

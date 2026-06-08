package com.optimapc.backend.catalogo;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.optimapc.backend.modelo.TipoUso;
import com.optimapc.backend.modelo.Premontado;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.usuario.PerfilUsuarioService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/catalogo/premontados")
public class CatalogoController {

    private final PremontadoCatalogoService premontadoCatalogoService;
    private final PerfilUsuarioService perfilUsuarioService;
    private final ScoringService scoringService;
    private final ChatService chatService;
    private final RendimientoService rendimientoService;

    public CatalogoController(
            PremontadoCatalogoService premontadoCatalogoService,
            PerfilUsuarioService perfilUsuarioService,
            ScoringService scoringService,
            ChatService chatService,
            RendimientoService rendimientoService) {
        this.premontadoCatalogoService = premontadoCatalogoService;
        this.perfilUsuarioService = perfilUsuarioService;
        this.scoringService = scoringService;
        this.chatService = chatService;
        this.rendimientoService = rendimientoService;
    }

    @GetMapping
    public List<PremontadoCatalogoDto> listarPremontados(
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Set<String> tipos,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Boolean reacondicionado) {
        return premontadoCatalogoService.listar(
                minPrice,
                maxPrice,
                tipos != null ? tipos.stream()
                        .map(String::toUpperCase)
                        .map(s -> {
                            try {
                                return TipoUso.valueOf(s);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(t -> t != null)
                        .toList()
                        : null,
                marca,
                reacondicionado);
    }

    @GetMapping("/recomendaciones")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PremontadoCatalogoDto>> obtenerRecomendaciones(Authentication authentication) {
        Long usuarioId = (Long) authentication.getPrincipal();

        return perfilUsuarioService.obtenerPorUsuarioId(usuarioId)
                .map(perfil -> {
                    List<Premontado> premontados = premontadoCatalogoService.obtenerTodosLosPremontados();
                    rendimientoService.normalizarLista(premontados, perfil.getTipoUsoFrecuente());
                    List<Premontado> recomendados = scoringService.recomendarPremontados(perfil, premontados);
                    List<PremontadoCatalogoDto> recomendaciones = recomendados.stream()
                            .map(premontadoCatalogoService::toDto)
                            .toList();

                    return ResponseEntity.ok(recomendaciones);
                })
                .orElseGet(() -> ResponseEntity.ok(List.of()));
    }

    @GetMapping("/{id}/valoraciones")
    public List<ValoracionDto> obtenerValoraciones(@PathVariable Long id) {
        return premontadoCatalogoService.obtenerValoracionesDelProducto(id);
    }

    @PostMapping("/{id}/valoraciones")
    @Transactional
    public ResponseEntity<ValoracionDto> crearValoracion(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ValoracionRequest request) {
        Long usuarioId = (Long) authentication.getPrincipal();
        ValoracionDto valoracion = premontadoCatalogoService.crearValoracion(id, usuarioId, request);
        return ResponseEntity.status(201).body(valoracion);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PremontadoCatalogoDto> obtenerPremontado(@PathVariable Long id) {
        return premontadoCatalogoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatCatalogo(@Valid @RequestBody ChatRequest request) {
        String respuesta = chatService.generarRespuestaChat(request.mensaje(), request.historial());
        return ResponseEntity.ok(new ChatResponse(respuesta));
    }
}
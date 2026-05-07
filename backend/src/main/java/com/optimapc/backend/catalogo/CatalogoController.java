package com.optimapc.backend.catalogo;

import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.optimapc.backend.modelo.TipoUso;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final PremontadoCatalogoService premontadoCatalogoService;

    public CatalogoController(PremontadoCatalogoService premontadoCatalogoService) {
        this.premontadoCatalogoService = premontadoCatalogoService;
    }

    @GetMapping("/premontados")
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

    @GetMapping("/premontados/{id}/valoraciones")
    public List<ValoracionDto> obtenerValoraciones(@PathVariable Long id) {
        return premontadoCatalogoService.obtenerValoracionesDelProducto(id);
    }
}
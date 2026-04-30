package com.optimapc.backend.catalogo;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optimapc.backend.modelo.Premontado;
import com.optimapc.backend.modelo.TipoUso;
import com.optimapc.backend.modelo.Valoracion;

@Service
public class PremontadoCatalogoService {

    private final PremontadoRepository premontadoRepository;

    public PremontadoCatalogoService(PremontadoRepository premontadoRepository) {
        this.premontadoRepository = premontadoRepository;
    }

    @Transactional(readOnly = true)
    public List<PremontadoCatalogoDto> listar() {
        return listar(null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<PremontadoCatalogoDto> listar(
            Double minPrice,
            Double maxPrice,
            List<TipoUso> tipos,
            String marca,
            Boolean reacondicionado) {
        return premontadoRepository.findAllByOrderByMarcaAscIdAsc().stream()
                .filter(p -> {
                    Double precioEfectivo = getPrecioEfectivo(p);
                    return minPrice == null || precioEfectivo >= minPrice;
                })
                .filter(p -> {
                    Double precioEfectivo = getPrecioEfectivo(p);
                    return maxPrice == null || precioEfectivo <= maxPrice;
                })
                .filter(p -> marca == null || p.getMarca().equalsIgnoreCase(marca))
                .filter(p -> reacondicionado == null || p.getEsReacondicionado().equals(reacondicionado))
                .filter(p -> tipos == null || tipos.isEmpty() ||
                        p.getUsosPrevistos().stream().anyMatch(tipos::contains))
                .map(this::toDto)
                .sorted(Comparator.comparing(PremontadoCatalogoDto::valoracionMedia, Comparator.reverseOrder())
                        .thenComparing(PremontadoCatalogoDto::descuento, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PremontadoCatalogoDto::titulo))
                .collect(Collectors.toList());
    }

    private Double getPrecioEfectivo(Premontado premontado) {
        Double precioReducido = premontado.getPrecioReducido();
        return precioReducido != null ? precioReducido : premontado.getPrecio();
    }

    private PremontadoCatalogoDto toDto(Premontado premontado) {
        List<Valoracion> valoraciones = premontado.getValoraciones();
        double valoracionMedia = valoraciones.isEmpty()
                ? 0.0
                : valoraciones.stream().mapToInt(Valoracion::getPuntuacion).average().orElse(0.0);

        return new PremontadoCatalogoDto(
                premontado.getId(),
                construirTitulo(premontado),
                premontado.getDescripcion(),
                premontado.getMarca(),
                premontado.getDescuento(),
                premontado.getSistemaOperativo() != null ? premontado.getSistemaOperativo().name() : null,
                premontado.getStock(),
                premontado.getUsosPrevistos().stream().map(Enum::name).collect(Collectors.toList()),
                premontado.getImagenUrl(),
                premontado.getEsReacondicionado(),
                premontado.getPrecio(),
                premontado.getPrecioReducido(),
                redondear(valoracionMedia),
                valoraciones.size(),
                premontado.getFavorita(),
                premontado.getRendimientoPorEuro());
    }

    private String construirTitulo(Premontado premontado) {
        String tipoUso = premontado.getTipoUsoPrevisto();
        String marca = premontado.getMarca();
        if (tipoUso == null || tipoUso.isBlank()) {
            return marca;
        }

        return String.format(Locale.ROOT, "%s %s", marca, tipoUso).trim();
    }

    private double redondear(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
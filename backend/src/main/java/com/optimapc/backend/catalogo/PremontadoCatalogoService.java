package com.optimapc.backend.catalogo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.modelo.ConfiguracionComponente;
import com.optimapc.backend.modelo.Premontado;
import com.optimapc.backend.modelo.TipoUso;
import com.optimapc.backend.modelo.Valoracion;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class PremontadoCatalogoService {

    private final PremontadoRepository premontadoRepository;
    private final ValoracionRepository valoracionRepository;
    private final UsuarioRepository usuarioRepository;

    public PremontadoCatalogoService(
            PremontadoRepository premontadoRepository,
            ValoracionRepository valoracionRepository,
            UsuarioRepository usuarioRepository) {
        this.premontadoRepository = premontadoRepository;
        this.valoracionRepository = valoracionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // -------------------------------------------------------------------------
    // Catálogo
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PremontadoCatalogoDto> listar() {
        return listar(null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<Premontado> obtenerTodosLosPremontados() {
        List<Premontado> premontados = premontadoRepository.findAllByOrderByMarcaAscIdAsc();
        inicializarColecciones(premontados);
        return premontados;
    }

    @Transactional(readOnly = true)
    public List<PremontadoCatalogoDto> listar(
            Double minPrice,
            Double maxPrice,
            List<TipoUso> tipos,
            String marca,
            Boolean reacondicionado) {
        List<Premontado> premontados = premontadoRepository.findAllByOrderByMarcaAscIdAsc();
        inicializarColecciones(premontados);

        return premontados.stream()
                .filter(p -> minPrice == null || getPrecioEfectivo(p) >= minPrice)
                .filter(p -> maxPrice == null || getPrecioEfectivo(p) <= maxPrice)
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

    public PremontadoCatalogoDto toDto(Premontado premontado) {
        inicializarColecciones(premontado);

        List<Valoracion> valoraciones = premontado.getValoraciones();
        double valoracionMedia = valoraciones.isEmpty()
                ? 0.0
                : valoraciones.stream().mapToInt(Valoracion::getPuntuacion).average().orElse(0.0);

        List<ComponenteDto> componentesDto = premontado.getComponentes().stream()
                .map(cfg -> new ComponenteDto(
                        cfg.getComponente().getId(),
                        cfg.getCategoria(),
                        cfg.getComponente().getNombre(),
                        cfg.getComponente().getNombre(),
                        cfg.getComponente().getPrecio(),
                        cfg.getCantidad()))
                .collect(Collectors.toList());

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
                premontado.getRendimientoPorEuro(),
                componentesDto);
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

    // -------------------------------------------------------------------------
    // Valoraciones
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ValoracionDto> obtenerValoracionesDelProducto(Long premontadoId) {
        Premontado premontado = premontadoRepository.findById(premontadoId).orElse(null);
        if (premontado == null) {
            return List.of();
        }

        return premontado.getValoraciones().stream()
                .sorted((v1, v2) -> v2.getFecha().compareTo(v1.getFecha()))
                .map(v -> new ValoracionDto(
                        v.getId(),
                        v.getUsuario().getNombre() + " " + v.getUsuario().getApellidos(),
                        v.getPuntuacion(),
                        v.getComentario(),
                        v.getFecha()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PremontadoCatalogoDto> obtenerPorId(Long id) {
        return premontadoRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public ValoracionDto crearValoracion(Long premontadoId, Long usuarioId, ValoracionRequest request) {
        if (usuarioId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Premontado premontado = premontadoRepository.findById(premontadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Premontado no encontrado"));

        if (valoracionRepository.existsByUsuario_IdAndPremontado_Id(usuarioId, premontadoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya has valorado este premontado");
        }

        Valoracion valoracion = new Valoracion();
        valoracion.setUsuario(usuario);
        valoracion.setPremontado(premontado);
        valoracion.setPuntuacion(request.puntuacion());
        valoracion.setComentario(normalizarComentario(request.comentario()));
        valoracion.setFecha(LocalDateTime.now());

        Valoracion guardada = valoracionRepository.save(valoracion);
        usuario.getValoraciones().add(guardada);
        premontado.getValoraciones().add(guardada);

        return new ValoracionDto(
                guardada.getId(),
                usuario.getNombre() + " " + usuario.getApellidos(),
                guardada.getPuntuacion(),
                guardada.getComentario(),
                guardada.getFecha());
    }

    private String normalizarComentario(String comentario) {
        if (comentario == null) {
            return null;
        }
        String trimmed = comentario.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void inicializarColecciones(List<Premontado> premontados) {
        premontados.forEach(this::inicializarColecciones);
    }

    private void inicializarColecciones(Premontado premontado) {
        premontado.getValoraciones().size();
        premontado.getUsosPrevistos().size();
        premontado.getComponentes().forEach(cfg -> {
            if (cfg.getComponente() != null) {
                cfg.getComponente().getNombre();
                cfg.getComponente().getPrecio();
            }
        });
    }
}

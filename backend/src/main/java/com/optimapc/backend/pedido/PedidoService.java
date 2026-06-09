package com.optimapc.backend.pedido;

import java.util.List;
import java.util.Locale;

import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.modelo.ConfiguracionPC;
import com.optimapc.backend.modelo.ItemPedido;
import com.optimapc.backend.modelo.Pedido;
import com.optimapc.backend.modelo.Premontado;
import com.optimapc.backend.modelo.TipoUso;
import com.optimapc.backend.usuario.PerfilUsuarioRepository;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ConfiguracionPCRepository configuracionPCRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ConfiguracionPCRepository configuracionPCRepository,
            UsuarioRepository usuarioRepository,
            PerfilUsuarioRepository perfilUsuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.configuracionPCRepository = configuracionPCRepository;
        this.usuarioRepository = usuarioRepository;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
    }

    @Transactional
    public PedidoDto crear(Long usuarioId, CrearPedidoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);

        double total = 0.0;

        for (ItemPedidoRequest itemReq : request.items()) {
            ConfiguracionPC configuracion = configuracionPCRepository.findById(itemReq.configuracionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Configuración no encontrada: " + itemReq.configuracionId()));

            double precioUnitario = configuracion.getPrecioEfectivo();
            String nombreProducto = resolverNombre(configuracion);

            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setConfiguracion(configuracion);
            item.setCantidad(itemReq.cantidad());
            item.setPrecioUnitario(precioUnitario);
            item.setNombreProducto(nombreProducto);

            pedido.getItems().add(item);
            total += item.calcularSubtotal();
        }

        pedido.setTotal(total);
        Pedido guardado = pedidoRepository.save(pedido);

        actualizarPerfil(usuarioId, guardado);

        return toDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> listarDeUsuario(Long usuarioId) {
        return pedidoRepository.findAllByUsuario_IdOrderByFechaDesc(usuarioId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDto obtenerPorId(Long usuarioId, Long pedidoId) {
        return pedidoRepository.findByIdAndUsuario_Id(pedidoId, usuarioId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void actualizarPerfil(Long usuarioId, Pedido pedido) {
        if (pedido.getItems().isEmpty()) return;

        perfilUsuarioRepository.findByUsuario_Id(usuarioId).ifPresent(perfil -> {
            for (ItemPedido item : pedido.getItems()) {
                ConfiguracionPC unproxied = (ConfiguracionPC) Hibernate.unproxy(item.getConfiguracion());

                TipoUso usoInferido = null;
                Boolean esReacondicionado = false;

                if (unproxied instanceof Premontado p) {
                    usoInferido = p.getUsosPrevistos().stream().findFirst().orElse(null);
                    esReacondicionado = p.getEsReacondicionado();
                }

                perfil.actualizarDesdeCompra(usoInferido, item.getPrecioUnitario(), esReacondicionado);
            }
            perfilUsuarioRepository.save(perfil);
        });
    }

    private String resolverNombre(ConfiguracionPC configuracion) {
        ConfiguracionPC unproxied = (ConfiguracionPC) Hibernate.unproxy(configuracion);
        if (unproxied instanceof Premontado p) {
            String nombre = p.getNombre();
            String marca = p.getMarca();
            if (nombre == null || nombre.isBlank()) return marca;
            return String.format(Locale.ROOT, "%s %s", marca, nombre).trim();
        }
        String nombreConfig = unproxied.getNombreConfiguracion();
        if (nombreConfig != null && !nombreConfig.isBlank()) return nombreConfig;
        return "Configuración personalizada #" + configuracion.getId();
    }

    private PedidoDto toDto(Pedido pedido) {
        List<ItemPedidoDto> itemsDto = pedido.getItems().stream()
                .map(item -> new ItemPedidoDto(
                        item.getId(),
                        item.getConfiguracion().getId(),
                        item.getNombreProducto(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.calcularSubtotal()))
                .toList();

        return new PedidoDto(pedido.getId(), pedido.getFecha(), pedido.getTotal(), itemsDto);
    }
}

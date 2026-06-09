package com.optimapc.backend.montarPC;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.ConfiguracionComponente;
import com.optimapc.backend.modelo.ConfiguracionPC;
import com.optimapc.backend.pedido.ConfiguracionPCRepository;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class MisConfiguracionesService {

    private final ConfiguracionPCRepository configuracionPCRepository;
    private final ComponenteRepository componenteRepository;
    private final UsuarioRepository usuarioRepository;

    public MisConfiguracionesService(
            ConfiguracionPCRepository configuracionPCRepository,
            ComponenteRepository componenteRepository,
            UsuarioRepository usuarioRepository) {
        this.configuracionPCRepository = configuracionPCRepository;
        this.componenteRepository = componenteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public MiConfiguracionDto guardar(Long usuarioId, List<Long> componenteIds, String nombre) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Componente> componentes = componenteRepository.findAllById(componenteIds);
        if (componentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se encontraron los componentes indicados");
        }

        ConfiguracionPC config = new ConfiguracionPC();
        config.setUsuario(usuario);
        config.setNombreConfiguracion(nombre);

        for (Componente c : componentes) {
            ConfiguracionComponente cc = new ConfiguracionComponente();
            cc.setCategoria(MontarPCService.toTypeLabel(c));
            cc.setCantidad(1);
            cc.setComponente(c);
            config.agregarComponente(cc);
        }

        return toDto(configuracionPCRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<MiConfiguracionDto> listar(Long usuarioId) {
        return configuracionPCRepository.findAllByUsuario_IdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        ConfiguracionPC config = configuracionPCRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuración no encontrada"));

        if (config.getUsuario() == null || !config.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar esta configuración");
        }

        try {
            configuracionPCRepository.delete(config);
            configuracionPCRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puedes eliminar una configuración que ya ha sido pedida");
        }
    }

    private MiConfiguracionDto toDto(ConfiguracionPC config) {
        List<MiConfiguracionComponenteDto> comps = config.getComponentes().stream()
                .filter(cc -> cc.getComponente() != null)
                .map(cc -> new MiConfiguracionComponenteDto(
                        cc.getComponente().getId(),
                        cc.getCategoria(),
                        cc.getComponente().getNombre(),
                        cc.getComponente().getPrecio()))
                .toList();

        return new MiConfiguracionDto(
                config.getId(),
                config.getNombreConfiguracion(),
                config.getPrecio(),
                config.getFechaCreacion(),
                comps);
    }
}

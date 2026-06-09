package com.optimapc.backend.catalogo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.modelo.Favorito;
import com.optimapc.backend.modelo.Premontado;
import com.optimapc.backend.modelo.TipoUso;
import com.optimapc.backend.usuario.PerfilUsuarioRepository;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final PremontadoRepository premontadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PremontadoCatalogoService premontadoCatalogoService;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public FavoritoService(
            FavoritoRepository favoritoRepository,
            PremontadoRepository premontadoRepository,
            UsuarioRepository usuarioRepository,
            PremontadoCatalogoService premontadoCatalogoService,
            PerfilUsuarioRepository perfilUsuarioRepository) {
        this.favoritoRepository = favoritoRepository;
        this.premontadoRepository = premontadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.premontadoCatalogoService = premontadoCatalogoService;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
    }

    @Transactional
    public FavoritoDto añadir(Long usuarioId, Long premontadoId) {
        if (favoritoRepository.existsByUsuario_IdAndPremontado_Id(usuarioId, premontadoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este premontado ya está en favoritos");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Premontado premontado = premontadoRepository.findById(premontadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Premontado no encontrado"));

        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setPremontado(premontado);

        Favorito guardado = favoritoRepository.save(favorito);

        perfilUsuarioRepository.findByUsuario_Id(usuarioId).ifPresent(perfil -> {
            TipoUso usoInferido = premontado.getUsosPrevistos().stream().findFirst().orElse(null);
            perfil.actualizarDesdeFavorito(usoInferido, premontado.getPrecioEfectivo(), premontado.getEsReacondicionado());
            perfilUsuarioRepository.save(perfil);
        });

        return toDto(guardado);
    }

    @Transactional
    public void eliminar(Long usuarioId, Long premontadoId) {
        if (!favoritoRepository.existsByUsuario_IdAndPremontado_Id(usuarioId, premontadoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El premontado no está en favoritos");
        }

        Premontado premontado = premontadoRepository.findById(premontadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Premontado no encontrado"));

        perfilUsuarioRepository.findByUsuario_Id(usuarioId).ifPresent(perfil -> {
            perfil.revertirDesdeFavorito(premontado.getEsReacondicionado());
            perfilUsuarioRepository.save(perfil);
        });

        favoritoRepository.deleteByUsuario_IdAndPremontado_Id(usuarioId, premontadoId);
    }

    @Transactional(readOnly = true)
    public List<FavoritoDto> listarDeUsuario(Long usuarioId) {
        return favoritoRepository.findAllByUsuario_IdOrderByFechaGuardadoDesc(usuarioId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean esFavorito(Long usuarioId, Long premontadoId) {
        return favoritoRepository.existsByUsuario_IdAndPremontado_Id(usuarioId, premontadoId);
    }

    private FavoritoDto toDto(Favorito favorito) {
        PremontadoCatalogoDto premontadoDto = premontadoCatalogoService.toDto(favorito.getPremontado());
        return new FavoritoDto(favorito.getId(), favorito.getFechaGuardado(), premontadoDto);
    }
}

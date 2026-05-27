package com.optimapc.backend.usuario;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.modelo.PerfilUsuario;
import com.optimapc.backend.usuario.dto.EncuestaInicialRequest;

@Service
public class PerfilUsuarioService {

    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PerfilUsuarioService(PerfilUsuarioRepository perfilUsuarioRepository, UsuarioRepository usuarioRepository) {
        this.perfilUsuarioRepository = perfilUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PerfilUsuario> obtenerPorUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }

        return perfilUsuarioRepository.findByUsuario_Id(usuarioId);
    }

    @Transactional
    public PerfilUsuario guardarEncuestaInicial(Long usuarioId, EncuestaInicialRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        PerfilUsuario perfil = perfilUsuarioRepository.findByUsuario_Id(usuarioId)
                .orElseGet(() -> {
                    PerfilUsuario nuevo = new PerfilUsuario();
                    nuevo.setUsuario(usuario);
                    return nuevo;
                });

        perfil.actualizarDesdeEncuesta(
                request.usoPrincipal(),
                request.usosSecundariosEncuesta(),
                request.presupuesto(),
                request.preferenciaReacondicionado());

        return perfilUsuarioRepository.save(perfil);
    }
}
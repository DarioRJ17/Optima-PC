package com.optimapc.backend.usuario;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.optimapc.backend.modelo.PerfilUsuario;

@Service
public class PerfilUsuarioService {

    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public PerfilUsuarioService(PerfilUsuarioRepository perfilUsuarioRepository) {
        this.perfilUsuarioRepository = perfilUsuarioRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PerfilUsuario> obtenerPorUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }

        return perfilUsuarioRepository.findByUsuario_Id(usuarioId);
    }
}
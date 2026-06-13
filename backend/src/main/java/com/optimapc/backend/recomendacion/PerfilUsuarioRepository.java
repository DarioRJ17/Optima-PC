package com.optimapc.backend.recomendacion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.domain.PerfilUsuario;

public interface PerfilUsuarioRepository extends JpaRepository<PerfilUsuario, Long> {

    Optional<PerfilUsuario> findByUsuario_Id(Long usuarioId);
}
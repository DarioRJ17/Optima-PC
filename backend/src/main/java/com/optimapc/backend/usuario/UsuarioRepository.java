package com.optimapc.backend.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);
}

package com.optimapc.backend.pedido;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.modelo.ConfiguracionPC;

public interface ConfiguracionPCRepository extends JpaRepository<ConfiguracionPC, Long> {

    Optional<ConfiguracionPC> findById(Long id);
}

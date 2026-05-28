package com.optimapc.backend.catalogo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.modelo.Valoracion;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    boolean existsByUsuario_IdAndPremontado_Id(Long usuarioId, Long premontadoId);

    Optional<Valoracion> findByUsuario_IdAndPremontado_Id(Long usuarioId, Long premontadoId);
}
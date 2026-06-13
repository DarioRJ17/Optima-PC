package com.optimapc.backend.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.domain.Favorito;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    boolean existsByUsuario_IdAndPremontado_Id(Long usuarioId, Long premontadoId);

    Optional<Favorito> findByUsuario_IdAndPremontado_Id(Long usuarioId, Long premontadoId);

    List<Favorito> findAllByUsuario_IdOrderByFechaGuardadoDesc(Long usuarioId);

    void deleteByUsuario_IdAndPremontado_Id(Long usuarioId, Long premontadoId);
}

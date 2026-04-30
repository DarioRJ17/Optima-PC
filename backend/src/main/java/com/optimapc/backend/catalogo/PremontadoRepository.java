package com.optimapc.backend.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.modelo.Premontado;

public interface PremontadoRepository extends JpaRepository<Premontado, Long> {

    @EntityGraph(attributePaths = {"valoraciones"})
    List<Premontado> findAllByOrderByMarcaAscIdAsc();
}
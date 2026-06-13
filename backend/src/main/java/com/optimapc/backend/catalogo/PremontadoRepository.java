package com.optimapc.backend.catalogo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.domain.Premontado;

public interface PremontadoRepository extends JpaRepository<Premontado, Long> {

    List<Premontado> findAllByOrderByMarcaAscIdAsc();
}
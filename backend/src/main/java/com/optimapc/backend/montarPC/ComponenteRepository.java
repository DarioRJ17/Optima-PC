package com.optimapc.backend.montarPC;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.modelo.Componente;

public interface ComponenteRepository extends JpaRepository<Componente, Long> {
    
}

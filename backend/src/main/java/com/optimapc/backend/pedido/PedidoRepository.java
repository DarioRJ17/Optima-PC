package com.optimapc.backend.pedido;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.optimapc.backend.domain.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByUsuario_IdOrderByFechaDesc(Long usuarioId);

    Optional<Pedido> findByIdAndUsuario_Id(Long pedidoId, Long usuarioId);
}

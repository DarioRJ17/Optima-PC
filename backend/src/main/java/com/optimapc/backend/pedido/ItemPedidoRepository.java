package com.optimapc.backend.pedido;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.optimapc.backend.domain.ItemPedido;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    @Query("SELECT i.configuracion.id, SUM(i.cantidad) FROM ItemPedido i WHERE TYPE(i.configuracion) = Premontado GROUP BY i.configuracion.id")
    List<Object[]> sumComprasPorPremontado();
}

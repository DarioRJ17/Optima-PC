package com.optimapc.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.Pedido;
import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void listaYBuscaPedidosDelUsuario() {
        Usuario usuario = persistUsuario("ana@test.com");
        Usuario otro = persistUsuario("otro@test.com");
        Pedido pedido = persistPedido(usuario);

        assertThat(pedidoRepository.findAllByUsuario_IdOrderByFechaDesc(usuario.getId())).hasSize(1);
        assertThat(pedidoRepository.findByIdAndUsuario_Id(pedido.getId(), usuario.getId())).isPresent();
        // Un pedido de otro usuario no debe ser accesible
        assertThat(pedidoRepository.findByIdAndUsuario_Id(pedido.getId(), otro.getId())).isEmpty();
    }

    private Usuario persistUsuario(String email) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return em.persistAndFlush(u);
    }

    private Pedido persistPedido(Usuario usuario) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setTotal(1170.0);
        return em.persistAndFlush(pedido);
    }
}

package com.optimapc.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.ItemPedido;
import com.optimapc.backend.domain.Pedido;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class ItemPedidoRepositoryTest {

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void sumComprasPorPremontadoAgregaSoloLosPremontados() {
        Usuario usuario = persistUsuario();
        Premontado premontado = persistPremontado("Asus");
        ConfiguracionPC personalizada = persistConfiguracion(usuario); // NO es premontado

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setTotal(0.0);
        em.persist(pedido);

        em.persist(item(pedido, premontado, 2));   // premontado: cuenta
        em.persist(item(pedido, premontado, 3));   // premontado: cuenta (mismo id -> suma 5)
        em.persist(item(pedido, personalizada, 4)); // personalizada: la query la excluye por TYPE()
        em.flush();

        List<Object[]> filas = itemPedidoRepository.sumComprasPorPremontado();

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0)[0]).isEqualTo(premontado.getId());
        assertThat(((Number) filas.get(0)[1]).longValue()).isEqualTo(5L);
    }

    private ItemPedido item(Pedido pedido, ConfiguracionPC config, int cantidad) {
        ItemPedido item = new ItemPedido();
        item.setPedido(pedido);
        item.setConfiguracion(config);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(1000.0);
        item.setNombreProducto("snapshot");
        return item;
    }

    private Usuario persistUsuario() {
        Usuario u = new Usuario();
        u.setEmail("ana@test.com");
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return em.persistAndFlush(u);
    }

    private Premontado persistPremontado(String marca) {
        Premontado p = new Premontado();
        p.setNombre("Equipo");
        p.setMarca(marca);
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(3);
        p.setEsReacondicionado(false);
        return em.persistAndFlush(p);
    }

    private ConfiguracionPC persistConfiguracion(Usuario usuario) {
        ConfiguracionPC c = new ConfiguracionPC();
        c.setUsuario(usuario);
        c.setNombreConfiguracion("Mi build");
        return em.persistAndFlush(c);
    }
}

package com.optimapc.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class ConfiguracionPCRepositoryTest {

    @Autowired
    private ConfiguracionPCRepository configuracionPCRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void listaLasConfiguracionesDelUsuarioMasRecientesPrimero() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@test.com");
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        usuario.setPassword("hash");
        em.persistAndFlush(usuario);

        persistConfig(usuario, "Antigua", LocalDateTime.now().minusDays(2));
        persistConfig(usuario, "Reciente", LocalDateTime.now());

        assertThat(configuracionPCRepository.findAllByUsuario_IdOrderByFechaCreacionDesc(usuario.getId()))
                .extracting(ConfiguracionPC::getNombreConfiguracion)
                .containsExactly("Reciente", "Antigua");
    }

    private void persistConfig(Usuario usuario, String nombre, LocalDateTime fecha) {
        ConfiguracionPC c = new ConfiguracionPC();
        c.setUsuario(usuario);
        c.setNombreConfiguracion(nombre);
        c.setFechaCreacion(fecha);
        em.persistAndFlush(c);
    }
}

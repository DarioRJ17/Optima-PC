package com.optimapc.backend.recomendacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class PerfilUsuarioRepositoryTest {

    @Autowired
    private PerfilUsuarioRepository perfilUsuarioRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void encuentraPerfilPorIdDeUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@test.com");
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        usuario.setPassword("hash");
        em.persistAndFlush(usuario);

        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setUsuario(usuario);
        perfil.setTipoUsoFrecuente(TipoUso.GAMING);
        em.persistAndFlush(perfil);

        assertThat(perfilUsuarioRepository.findByUsuario_Id(usuario.getId()))
                .isPresent()
                .get()
                .extracting(PerfilUsuario::getTipoUsoFrecuente)
                .isEqualTo(TipoUso.GAMING);
        assertThat(perfilUsuarioRepository.findByUsuario_Id(9999L)).isEmpty();
    }
}

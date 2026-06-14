package com.optimapc.backend.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void seed() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@test.com");
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        usuario.setPassword("hash");
        entityManager.persistAndFlush(usuario);
    }

    @Test
    void existePorEmailIgnorandoMayusculas() {
        assertThat(usuarioRepository.existsByEmailIgnoreCase("ANA@TEST.COM")).isTrue();
        assertThat(usuarioRepository.existsByEmailIgnoreCase("otro@test.com")).isFalse();
    }

    @Test
    void buscaPorEmailIgnorandoMayusculas() {
        assertThat(usuarioRepository.findByEmailIgnoreCase("Ana@Test.com"))
                .isPresent()
                .get()
                .extracting(Usuario::getNombre)
                .isEqualTo("Ana");
        assertThat(usuarioRepository.findByEmailIgnoreCase("nadie@test.com")).isEmpty();
    }
}

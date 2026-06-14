package com.optimapc.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository repository;
    @Autowired
    private TestEntityManager em;

    @Test
    void encuentraTokenVigenteYNoUsado() {
        Usuario usuario = persistUsuario();
        persistToken(usuario, "hash-vigente", LocalDateTime.now().plusHours(1), null);

        assertThat(repository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter("hash-vigente", LocalDateTime.now()))
                .isPresent();
    }

    @Test
    void noEncuentraTokenExpiradoNiUsado() {
        Usuario usuario = persistUsuario();
        persistToken(usuario, "hash-expirado", LocalDateTime.now().minusMinutes(1), null);
        persistToken(usuario, "hash-usado", LocalDateTime.now().plusHours(1), LocalDateTime.now());

        assertThat(repository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter("hash-expirado", LocalDateTime.now())).isEmpty();
        assertThat(repository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter("hash-usado", LocalDateTime.now())).isEmpty();
    }

    @Test
    void borraTokensNoUsadosDelUsuario() {
        Usuario usuario = persistUsuario();
        persistToken(usuario, "h1", LocalDateTime.now().plusHours(1), null);
        persistToken(usuario, "h2", LocalDateTime.now().plusHours(1), null);
        persistToken(usuario, "h3-usado", LocalDateTime.now().plusHours(1), LocalDateTime.now());

        long borrados = repository.deleteByUsuarioIdAndUsedAtIsNull(usuario.getId());
        em.flush();

        assertThat(borrados).isEqualTo(2);
    }

    private Usuario persistUsuario() {
        Usuario u = new Usuario();
        u.setEmail("ana@test.com");
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return em.persistAndFlush(u);
    }

    private void persistToken(Usuario usuario, String hash, LocalDateTime expiresAt, LocalDateTime usedAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuario(usuario);
        token.setTokenHash(hash);
        token.setRequestedAt(LocalDateTime.now());
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        em.persistAndFlush(token);
    }
}

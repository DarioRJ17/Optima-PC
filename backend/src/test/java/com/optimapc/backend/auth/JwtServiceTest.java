package com.optimapc.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "clave-secreta-de-pruebas-con-mas-de-32-caracteres");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86400000L);
    }

    @Test
    void tokenGeneradoPermiteExtraerUsuarioEmailYTipo() {
        String token = jwtService.generateToken(42L, "user@test.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("auth");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void tokenDeRecuperacionTieneTipoPasswordReset() {
        String token = jwtService.generatePasswordResetToken(1L, "user@test.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("password-reset");
    }

    @Test
    void tokenInvalidoLanzaExcepcionAlExtraerUsuario() {
        assertThatThrownBy(() -> jwtService.extractUserId("no-es-un-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenInvalidoLanzaExcepcionAlExtraerEmail() {
        assertThatThrownBy(() -> jwtService.extractEmail("no-es-un-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tipoDeTokenInvalidoDevuelveNull() {
        assertThat(jwtService.extractTokenType("basura")).isNull();
    }

    @Test
    void tokenBasuraNoEsValido() {
        assertThat(jwtService.isTokenValid("basura")).isFalse();
    }

    @Test
    void tokenExpiradoNoEsValidoYLanzaAlExtraer() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // ya expirado
        String expirado = jwtService.generateToken(1L, "user@test.com");
        assertThat(jwtService.isTokenValid(expirado)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUserId(expirado))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.optimapc.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.auth.dto.AuthResponse;
import com.optimapc.backend.auth.dto.LoginRequest;
import com.optimapc.backend.auth.dto.PasswordResetRequest;
import com.optimapc.backend.auth.dto.PasswordStrength;
import com.optimapc.backend.auth.dto.RegisterRequest;
import com.optimapc.backend.auth.dto.ResetPasswordRequest;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService,
                passwordResetTokenRepository, passwordResetEmailService);
    }

    private Usuario usuarioGuardado() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("user@test.com");
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return u;
    }

    @Test
    void registroCreaUsuarioYDevuelveToken() {
        RegisterRequest request = new RegisterRequest("Ana", "García", "User@Test.com", "Segura1!");
        when(usuarioRepository.existsByEmailIgnoreCase("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Segura1!")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado());
        when(jwtService.generateToken(1L, "user@test.com")).thenReturn("jwt-token");

        AuthResponse respuesta = authService.register(request);

        assertThat(respuesta.token()).isEqualTo("jwt-token");
        assertThat(respuesta.email()).isEqualTo("user@test.com");
    }

    @Test
    void registroConEmailExistenteLanzaConflict() {
        RegisterRequest request = new RegisterRequest("Ana", "García", "user@test.com", "Segura1!");
        when(usuarioRepository.existsByEmailIgnoreCase("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ya existe");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginCorrectoDevuelveToken() {
        LoginRequest request = new LoginRequest("user@test.com", "Segura1!");
        when(usuarioRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(usuarioGuardado()));
        when(passwordEncoder.matches("Segura1!", "hash")).thenReturn(true);
        when(jwtService.generateToken(1L, "user@test.com")).thenReturn("jwt-token");

        assertThat(authService.login(request).token()).isEqualTo("jwt-token");
    }

    @Test
    void loginConEmailDesconocidoLanzaUnauthorized() {
        LoginRequest request = new LoginRequest("nadie@test.com", "x");
        when(usuarioRepository.findByEmailIgnoreCase("nadie@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    @Test
    void loginConPasswordIncorrectaLanzaUnauthorized() {
        LoginRequest request = new LoginRequest("user@test.com", "mala");
        when(usuarioRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(usuarioGuardado()));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    @Test
    void solicitudDeResetConUsuarioExistenteGuardaTokenYEnviaCorreo() {
        PasswordResetRequest request = new PasswordResetRequest("user@test.com");
        when(usuarioRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(usuarioGuardado()));

        authService.requestPasswordReset(request);

        verify(passwordResetTokenRepository).deleteByUsuarioIdAndUsedAtIsNull(1L);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(passwordResetEmailService).sendResetLink(anyString(), anyString());
    }

    @Test
    void solicitudDeResetConUsuarioDesconocidoNoHaceNada() {
        PasswordResetRequest request = new PasswordResetRequest("nadie@test.com");
        when(usuarioRepository.findByEmailIgnoreCase("nadie@test.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset(request);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetEmailService, never()).sendResetLink(anyString(), anyString());
    }

    @Test
    void resetConTokenValidoCambiaLaPassword() {
        Usuario usuario = usuarioGuardado();
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuario(usuario);
        token.setTokenHash("hash-token");

        ResetPasswordRequest request = new ResetPasswordRequest("token-crudo", "NuevaSegura1!");
        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NuevaSegura1!")).thenReturn("nuevo-hash");

        authService.resetPassword(request);

        assertThat(usuario.getPassword()).isEqualTo("nuevo-hash");
        assertThat(token.getUsedAt()).isNotNull();
        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenRepository).deleteByUsuarioIdAndUsedAtIsNull(1L);
    }

    @Test
    void resetConTokenInvalidoLanzaBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest("malo", "NuevaSegura1!");
        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no es válido o ha expirado");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void evaluaFortalezaDeContrasena() {
        assertThat(authService.evaluate(null)).isEqualTo(PasswordStrength.VERY_WEAK);
        assertThat(authService.evaluate("")).isEqualTo(PasswordStrength.VERY_WEAK);
        assertThat(authService.evaluate("123")).isEqualTo(PasswordStrength.VERY_WEAK);
        assertThat(authService.evaluate("lmnopqrs")).isEqualTo(PasswordStrength.WEAK);
        assertThat(authService.evaluate("Abcdef12")).isEqualTo(PasswordStrength.FAIR);
        assertThat(authService.evaluate("Mnopqrstuv9!@#Yz")).isEqualTo(PasswordStrength.VERY_STRONG);
    }
}

package com.optimapc.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.optimapc.backend.auth.AuthController.PasswordCheckRequest;
import com.optimapc.backend.auth.dto.AuthResponse;
import com.optimapc.backend.auth.dto.LoginRequest;
import com.optimapc.backend.auth.dto.PasswordResetRequest;
import com.optimapc.backend.auth.dto.PasswordStrength;
import com.optimapc.backend.auth.dto.RegisterRequest;
import com.optimapc.backend.auth.dto.ResetPasswordRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    private AuthResponse respuesta() {
        return new AuthResponse("ok", 1L, "Ana", "García", "user@test.com", null, "jwt");
    }

    @Test
    void registroDevuelve201() {
        RegisterRequest request = new RegisterRequest("Ana", "García", "user@test.com", "Segura1!");
        when(authService.register(request)).thenReturn(respuesta());
        var http = controller.register(request);
        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(http.getBody().token()).isEqualTo("jwt");
    }

    @Test
    void loginDevuelve200() {
        LoginRequest request = new LoginRequest("user@test.com", "Segura1!");
        when(authService.login(request)).thenReturn(respuesta());
        assertThat(controller.login(request).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void solicitudDeResetDevuelveMensaje() {
        PasswordResetRequest request = new PasswordResetRequest("user@test.com");
        var http = controller.requestPasswordReset(request);
        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).requestPasswordReset(request);
    }

    @Test
    void confirmacionDeResetDevuelveMensaje() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NuevaSegura1!");
        var http = controller.confirmPasswordReset(request);
        assertThat(http.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).resetPassword(request);
    }

    @Test
    void compruebaFortalezaDeContrasena() {
        when(authService.evaluate("Segura1!")).thenReturn(PasswordStrength.STRONG);
        var http = controller.checkStrength(new PasswordCheckRequest("Segura1!"));
        assertThat(http.getBody().strength()).isEqualTo(PasswordStrength.STRONG);
    }
}

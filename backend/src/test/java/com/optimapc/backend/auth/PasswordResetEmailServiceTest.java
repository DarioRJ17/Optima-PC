package com.optimapc.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTest {

    @Mock
    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void sinMailSenderConfiguradoLanzaServiceUnavailable() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        PasswordResetEmailService service = new PasswordResetEmailService(mailSenderProvider, "http://localhost:5173", "");

        assertThatThrownBy(() -> service.sendResetLink("user@test.com", "raw-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void enviaCorreoConEnlaceDeReseteo() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        PasswordResetEmailService service = new PasswordResetEmailService(mailSenderProvider, "http://localhost:5173/", "noreply@optimapc.com");

        service.sendResetLink("user@test.com", "raw token/con+simbolos");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage enviado = captor.getValue();
        assertThat(enviado.getTo()).containsExactly("user@test.com");
        assertThat(enviado.getFrom()).isEqualTo("noreply@optimapc.com");
        assertThat(enviado.getText()).contains("/reset-password?token=");
    }

    @Test
    void siFallaElEnvioLanzaServiceUnavailable() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("smtp caído")).when(mailSender).send(any(SimpleMailMessage.class));
        PasswordResetEmailService service = new PasswordResetEmailService(mailSenderProvider, "http://localhost:5173", "");

        assertThatThrownBy(() -> service.sendResetLink("user@test.com", "raw-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se pudo enviar");
    }
}

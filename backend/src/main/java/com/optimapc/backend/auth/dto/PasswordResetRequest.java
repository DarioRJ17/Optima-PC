package com.optimapc.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es valido")
        String email) {
}
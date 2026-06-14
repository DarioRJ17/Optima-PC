package com.optimapc.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.optimapc.backend.auth.dto.StrongPasswordValidator;

import jakarta.validation.ConstraintValidatorContext;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);

    @Test
    void contrasenaFuerteEsValida() {
        assertThat(validator.isValid("Segura12!", context)).isTrue();
    }

    @Test
    void contrasenaNulaOEnBlancoEsInvalida() {
        assertThat(validator.isValid(null, context)).isFalse();
        assertThat(validator.isValid("   ", context)).isFalse();
    }

    @Test
    void contrasenaSinSimboloNiNumeroEsInvalida() {
        assertThat(validator.isValid("SoloLetras", context)).isFalse();
    }

    @Test
    void contrasenaCortaEsInvalida() {
        assertThat(validator.isValid("Ab1!", context)).isFalse();
    }
}

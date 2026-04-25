package com.optimapc.backend.auth.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "La contraseña no es suficientemente segura";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

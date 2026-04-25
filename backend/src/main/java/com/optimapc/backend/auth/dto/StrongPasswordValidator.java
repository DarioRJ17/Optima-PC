package com.optimapc.backend.auth.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) return false;

        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit     = password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol    = password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0);
        boolean hasMinLength = password.length() >= 8;

        List<String> errors = new ArrayList<>();
        if (!hasMinLength) errors.add("al menos 8 caracteres");
        if (!hasUpperCase) errors.add("una mayúscula");
        if (!hasLowerCase) errors.add("una minúscula");
        if (!hasDigit)     errors.add("un número");
        if (!hasSymbol)    errors.add("un símbolo (!@#$...)");

        if (!errors.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "La contraseña debe contener: " + String.join(", ", errors)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}

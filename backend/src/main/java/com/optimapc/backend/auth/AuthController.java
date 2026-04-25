package com.optimapc.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.optimapc.backend.auth.dto.AuthResponse;
import com.optimapc.backend.auth.dto.LoginRequest;
import com.optimapc.backend.auth.dto.PasswordStrength;
import com.optimapc.backend.auth.dto.RegisterRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password-strength")
    public ResponseEntity<PasswordStrengthResponse> checkStrength(
            @RequestBody @Valid PasswordCheckRequest request) {

        PasswordStrength strength = authService.evaluate(request.password());
        return ResponseEntity.ok(new PasswordStrengthResponse(strength));
    }

    public record PasswordCheckRequest(
        @NotBlank String password
    ) {}

    public record PasswordStrengthResponse(
        PasswordStrength strength   // "VERY_WEAK" | "WEAK" | "FAIR" | "STRONG" | "VERY_STRONG"
    ) {}
}

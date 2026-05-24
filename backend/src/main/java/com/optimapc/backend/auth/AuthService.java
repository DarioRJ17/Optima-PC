package com.optimapc.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.auth.dto.AuthResponse;
import com.optimapc.backend.auth.dto.LoginRequest;
import com.optimapc.backend.auth.dto.PasswordResetRequest;
import com.optimapc.backend.auth.dto.PasswordStrength;
import com.optimapc.backend.auth.dto.RegisterRequest;
import com.optimapc.backend.auth.dto.ResetPasswordRequest;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetEmailService passwordResetEmailService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetEmailService passwordResetEmailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetEmailService = passwordResetEmailService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre(request.nombre().trim());
        usuario.setApellidos(request.apellidos().trim());
        usuario.setPassword(passwordEncoder.encode(request.password()));

        Usuario guardado = usuarioRepository.save(usuario);
        String token = jwtService.generateToken(guardado.getId(), guardado.getEmail());

        return new AuthResponse(
                "Usuario registrado correctamente",
                guardado.getId(),
                guardado.getNombre(),
                guardado.getApellidos(),
                guardado.getEmail(),
                guardado.getFechaRegistro(),
                token);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail());

        return new AuthResponse(
                "Inicio de sesion correcto",
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidos(),
                usuario.getEmail(),
                usuario.getFechaRegistro(),
                token);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = request.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);

        if (usuario == null) {
            return;
        }

        passwordResetTokenRepository.deleteByUsuarioIdAndUsedAtIsNull(usuario.getId());

        String rawToken = generateResetToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuario(usuario);
        token.setTokenHash(hashToken(rawToken));
        token.setRequestedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        passwordResetTokenRepository.save(token);
        passwordResetEmailService.sendResetLink(email, rawToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = request.token().trim();
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hashToken(rawToken), LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace no es válido o ha expirado"));

        Usuario usuario = token.getUsuario();
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuarioRepository.save(usuario);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.deleteByUsuarioIdAndUsedAtIsNull(usuario.getId());
    }

    public PasswordStrength evaluate(String password) {
        if (password == null || password.isBlank()) return PasswordStrength.VERY_WEAK;

        int score = 0;

        // Longitud
        if (password.length() >= 8)  score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;

        // Variedad de caracteres
        if (password.chars().anyMatch(Character::isUpperCase)) score++;
        if (password.chars().anyMatch(Character::isLowerCase)) score++;
        if (password.chars().anyMatch(Character::isDigit))     score++;
        if (password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0)) score++;

        // Penalización por patrones comunes
        boolean hasCommonPattern = COMMON_PATTERNS.stream().anyMatch(p -> password.contains(p));
        if (hasCommonPattern) score -= 1;

        return switch (score) {
            case 0, 1    -> PasswordStrength.VERY_WEAK;
            case 2, 3    -> PasswordStrength.WEAK;
            case 4       -> PasswordStrength.FAIR;
            case 5, 6    -> PasswordStrength.STRONG;
            default      -> PasswordStrength.VERY_STRONG;
        };
    }

    private static final Set<String> COMMON_PATTERNS = Set.of(
        "123", "1234", "12345", "abc", "password", "contraseña",
        "qwerty", "admin", "user", "0000", "1111", "pass"
    );

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte current : hashed) {
                hex.append(String.format("%02x", current));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo generar el hash del token", exception);
        }
    }
}

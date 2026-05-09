package com.optimapc.backend.auth;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.auth.dto.AuthResponse;
import com.optimapc.backend.auth.dto.LoginRequest;
import com.optimapc.backend.auth.dto.PasswordStrength;
import com.optimapc.backend.auth.dto.RegisterRequest;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
}

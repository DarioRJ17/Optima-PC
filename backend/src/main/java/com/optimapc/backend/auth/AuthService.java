package com.optimapc.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.auth.dto.AuthResponse;
import com.optimapc.backend.auth.dto.LoginRequest;
import com.optimapc.backend.auth.dto.RegisterRequest;
import com.optimapc.backend.usuario.Usuario;
import com.optimapc.backend.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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

        return new AuthResponse(
                "Usuario registrado correctamente",
                guardado.getId(),
                guardado.getNombre(),
                guardado.getApellidos(),
                guardado.getEmail(),
                guardado.getFechaRegistro());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        return new AuthResponse(
                "Inicio de sesion correcto",
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellidos(),
            usuario.getEmail(),
            usuario.getFechaRegistro());
    }
}

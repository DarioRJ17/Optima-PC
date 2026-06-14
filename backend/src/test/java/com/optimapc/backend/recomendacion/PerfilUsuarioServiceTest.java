package com.optimapc.backend.recomendacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.recomendacion.dto.EncuestaInicialRequest;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PerfilUsuarioServiceTest {

    @Mock
    private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private PerfilUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new PerfilUsuarioService(perfilUsuarioRepository, usuarioRepository);
    }

    @Test
    void obtenerConIdNuloDevuelveVacio() {
        assertThat(service.obtenerPorUsuarioId(null)).isEmpty();
    }

    @Test
    void obtenerDelegaEnElRepositorio() {
        PerfilUsuario perfil = new PerfilUsuario();
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(perfil));
        assertThat(service.obtenerPorUsuarioId(7L)).containsSame(perfil);
    }

    @Test
    void guardarEncuestaConUsuarioInexistenteLanzaNotFound() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        EncuestaInicialRequest request = new EncuestaInicialRequest(TipoUso.GAMING, List.of(), 1000.0, false);
        assertThatThrownBy(() -> service.guardarEncuestaInicial(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void guardarEncuestaCreaPerfilNuevoSiNoExiste() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.empty());
        when(perfilUsuarioRepository.save(any(PerfilUsuario.class))).thenAnswer(inv -> inv.getArgument(0));

        EncuestaInicialRequest request = new EncuestaInicialRequest(
                TipoUso.GAMING, List.of(TipoUso.EDICION), 1200.0, true);

        PerfilUsuario resultado = service.guardarEncuestaInicial(7L, request);

        assertThat(resultado.getUsuario()).isSameAs(usuario);
        assertThat(resultado.getTipoUsoFrecuente()).isEqualTo(TipoUso.GAMING);
        assertThat(resultado.getUsosSecundarios()).containsExactly(TipoUso.EDICION);
        assertThat(resultado.getPresupuestoEstimado()).isEqualTo(1200.0);
        assertThat(resultado.getPreferenciaDeReacondicionado()).isTrue();
    }

    @Test
    void guardarEncuestaActualizaPerfilExistente() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        PerfilUsuario existente = new PerfilUsuario();
        existente.setUsuario(usuario);
        existente.setTipoUsoFrecuente(TipoUso.OFIMATICA);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(existente));
        when(perfilUsuarioRepository.save(any(PerfilUsuario.class))).thenAnswer(inv -> inv.getArgument(0));

        EncuestaInicialRequest request = new EncuestaInicialRequest(TipoUso.GAMING, List.of(), 800.0, false);

        PerfilUsuario resultado = service.guardarEncuestaInicial(7L, request);

        assertThat(resultado).isSameAs(existente);
        assertThat(resultado.getTipoUsoFrecuente()).isEqualTo(TipoUso.GAMING);
    }
}

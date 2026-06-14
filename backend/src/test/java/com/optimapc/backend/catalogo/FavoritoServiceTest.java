package com.optimapc.backend.catalogo;

import static com.optimapc.backend.support.TestData.premontadoCompleto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.catalogo.dto.FavoritoDto;
import com.optimapc.backend.domain.Favorito;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.recomendacion.PerfilUsuarioRepository;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private PremontadoRepository premontadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PremontadoCatalogoService premontadoCatalogoService;
    @Mock
    private PerfilUsuarioRepository perfilUsuarioRepository;

    private FavoritoService service;

    @BeforeEach
    void setUp() {
        service = new FavoritoService(favoritoRepository, premontadoRepository,
                usuarioRepository, premontadoCatalogoService, perfilUsuarioRepository);
    }

    @Test
    void anadirFavoritoDuplicadoLanzaConflict() {
        when(favoritoRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.añadir(7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ya está en favoritos");
    }

    @Test
    void anadirFavoritoConUsuarioInexistenteLanzaNotFound() {
        when(favoritoRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.añadir(7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void anadirFavoritoGuardaYActualizaPerfil() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        Premontado premontado = premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING);
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setUsuario(usuario);

        when(favoritoRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(premontado));
        when(favoritoRepository.save(any(Favorito.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(perfil));
        when(premontadoCatalogoService.toDto(premontado)).thenReturn(null);

        service.añadir(7L, 1L);

        verify(favoritoRepository).save(any(Favorito.class));
        verify(perfilUsuarioRepository).save(perfil);
    }

    @Test
    void eliminarFavoritoInexistenteLanzaNotFound() {
        when(favoritoRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        assertThatThrownBy(() -> service.eliminar(7L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no está en favoritos");
    }

    @Test
    void eliminarFavoritoRevierteElPerfilYBorra() {
        Premontado premontado = premontadoCompleto(1L, "Asus", "ROG", null, true, TipoUso.GAMING);
        PerfilUsuario perfil = new PerfilUsuario();

        when(favoritoRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(true);
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(premontado));
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(perfil));

        service.eliminar(7L, 1L);

        verify(perfilUsuarioRepository).save(perfil);
        verify(favoritoRepository).deleteByUsuario_IdAndPremontado_Id(7L, 1L);
    }

    @Test
    void listarFavoritosNormalizaYMapea() {
        Premontado premontado = premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING);
        Favorito favorito = new Favorito();
        favorito.setId(3L);
        favorito.setPremontado(premontado);
        when(favoritoRepository.findAllByUsuario_IdOrderByFechaGuardadoDesc(7L)).thenReturn(List.of(favorito));
        when(premontadoCatalogoService.toDto(premontado)).thenReturn(null);

        List<FavoritoDto> dtos = service.listarDeUsuario(7L);

        assertThat(dtos).hasSize(1);
        verify(premontadoCatalogoService).normalizarLista(List.of(premontado));
    }

    @Test
    void esFavoritoDelegaEnElRepositorio() {
        when(favoritoRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(true);
        assertThat(service.esFavorito(7L, 1L)).isTrue();
        verify(usuarioRepository, never()).findById(any());
    }
}

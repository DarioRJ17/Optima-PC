package com.optimapc.backend.montarPC;

import static com.optimapc.backend.support.TestData.config;
import static com.optimapc.backend.support.TestData.procesador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.montarPC.dto.MiConfiguracionDto;
import com.optimapc.backend.pedido.ConfiguracionPCRepository;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MisConfiguracionesServiceTest {

    @Mock
    private ConfiguracionPCRepository configuracionPCRepository;
    @Mock
    private ComponenteRepository componenteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private MisConfiguracionesService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new MisConfiguracionesService(configuracionPCRepository, componenteRepository, usuarioRepository);
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("u@test.com");
    }

    @Test
    void guardarConUsuarioInexistenteLanzaNotFound() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.guardar(7L, List.of(1L), "Mi PC"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void guardarSinComponentesLanzaBadRequest() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service.guardar(7L, List.of(1L), "Mi PC"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No se encontraron");
    }

    @Test
    void guardarPersisteYDevuelveDto() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(componenteRepository.findAllById(anyList())).thenReturn(List.of(procesador(1L, "AM5", 8, 5.0, 105, 300.0)));
        when(configuracionPCRepository.save(any(ConfiguracionPC.class))).thenAnswer(inv -> {
            ConfiguracionPC c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });

        MiConfiguracionDto dto = service.guardar(7L, List.of(1L), "Mi PC");

        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.nombre()).isEqualTo("Mi PC");
        assertThat(dto.componentes()).hasSize(1);
    }

    @Test
    void listarMapeaConfiguracionesDelUsuario() {
        ConfiguracionPC c = config(procesador(1L, "AM5", 8, 5.0, 105, 300.0));
        c.setId(60L);
        c.setUsuario(usuario);
        c.setNombreConfiguracion("Build");
        when(configuracionPCRepository.findAllByUsuario_IdOrderByFechaCreacionDesc(7L)).thenReturn(List.of(c));

        List<MiConfiguracionDto> dtos = service.listar(7L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).nombre()).isEqualTo("Build");
    }

    @Test
    void eliminarConfiguracionInexistenteLanzaNotFound() {
        when(configuracionPCRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.eliminar(99L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    void eliminarConfiguracionDeOtroUsuarioLanzaForbidden() {
        ConfiguracionPC c = new ConfiguracionPC();
        Usuario otro = new Usuario();
        otro.setId(99L);
        c.setUsuario(otro);
        when(configuracionPCRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.eliminar(1L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("permiso");
        verify(configuracionPCRepository, never()).delete(any());
    }

    @Test
    void eliminarConfiguracionPropiaLaBorra() {
        ConfiguracionPC c = new ConfiguracionPC();
        c.setUsuario(usuario);
        when(configuracionPCRepository.findById(1L)).thenReturn(Optional.of(c));

        service.eliminar(1L, 7L);

        verify(configuracionPCRepository).delete(c);
        verify(configuracionPCRepository).flush();
    }

    @Test
    void eliminarConfiguracionYaPedidaLanzaConflict() {
        ConfiguracionPC c = new ConfiguracionPC();
        c.setUsuario(usuario);
        when(configuracionPCRepository.findById(1L)).thenReturn(Optional.of(c));
        doThrow(new DataIntegrityViolationException("FK")).when(configuracionPCRepository).flush();

        assertThatThrownBy(() -> service.eliminar(1L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ya ha sido pedida");
    }
}

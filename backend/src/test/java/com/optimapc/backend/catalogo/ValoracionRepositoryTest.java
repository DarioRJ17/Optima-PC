package com.optimapc.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.domain.Valoracion;

@DataJpaTest
class ValoracionRepositoryTest {

    @Autowired
    private ValoracionRepository valoracionRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void detectaValoracionExistentePorUsuarioYPremontado() {
        Usuario usuario = persistUsuario();
        Premontado premontado = persistPremontado();
        Valoracion v = new Valoracion();
        v.setUsuario(usuario);
        v.setPremontado(premontado);
        v.setPuntuacion(5);
        v.setFecha(LocalDateTime.now());
        em.persistAndFlush(v);

        assertThat(valoracionRepository.existsByUsuario_IdAndPremontado_Id(usuario.getId(), premontado.getId())).isTrue();
        assertThat(valoracionRepository.findByUsuario_IdAndPremontado_Id(usuario.getId(), premontado.getId())).isPresent();
        assertThat(valoracionRepository.existsByUsuario_IdAndPremontado_Id(usuario.getId(), 9999L)).isFalse();
    }

    private Usuario persistUsuario() {
        Usuario u = new Usuario();
        u.setEmail("ana@test.com");
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setPassword("hash");
        return em.persistAndFlush(u);
    }

    private Premontado persistPremontado() {
        Premontado p = new Premontado();
        p.setNombre("Equipo");
        p.setMarca("Asus");
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(1);
        p.setEsReacondicionado(false);
        return em.persistAndFlush(p);
    }
}

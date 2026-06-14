package com.optimapc.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.Favorito;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.Usuario;

@DataJpaTest
class FavoritoRepositoryTest {

    @Autowired
    private FavoritoRepository favoritoRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void operacionesPorUsuarioYPremontado() {
        Usuario usuario = persistUsuario();
        Premontado premontado = persistPremontado();
        Favorito favorito = new Favorito();
        favorito.setUsuario(usuario);
        favorito.setPremontado(premontado);
        em.persistAndFlush(favorito);

        Long uId = usuario.getId();
        Long pId = premontado.getId();

        assertThat(favoritoRepository.existsByUsuario_IdAndPremontado_Id(uId, pId)).isTrue();
        assertThat(favoritoRepository.findByUsuario_IdAndPremontado_Id(uId, pId)).isPresent();
        assertThat(favoritoRepository.findAllByUsuario_IdOrderByFechaGuardadoDesc(uId)).hasSize(1);

        favoritoRepository.deleteByUsuario_IdAndPremontado_Id(uId, pId);
        em.flush();

        assertThat(favoritoRepository.existsByUsuario_IdAndPremontado_Id(uId, pId)).isFalse();
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
        p.setStock(3);
        p.setEsReacondicionado(false);
        return em.persistAndFlush(p);
    }
}

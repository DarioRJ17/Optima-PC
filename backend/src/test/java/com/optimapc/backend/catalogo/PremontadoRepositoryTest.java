package com.optimapc.backend.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.TipoSO;

@DataJpaTest
class PremontadoRepositoryTest {

    @Autowired
    private PremontadoRepository premontadoRepository;
    @Autowired
    private TestEntityManager em;

    @Test
    void devuelvePremontadosOrdenadosPorMarca() {
        persist("Zotac");
        persist("Asus");
        persist("HP");

        assertThat(premontadoRepository.findAllByOrderByMarcaAscIdAsc())
                .extracting(Premontado::getMarca)
                .containsExactly("Asus", "HP", "Zotac");
    }

    private void persist(String marca) {
        Premontado p = new Premontado();
        p.setNombre("Equipo");
        p.setMarca(marca);
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(1);
        p.setEsReacondicionado(false);
        em.persistAndFlush(p);
    }
}

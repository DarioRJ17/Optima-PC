package com.optimapc.backend.catalogo;

import static com.optimapc.backend.support.TestData.almacenamiento;
import static com.optimapc.backend.support.TestData.caja;
import static com.optimapc.backend.support.TestData.fuente;
import static com.optimapc.backend.support.TestData.gpu;
import static com.optimapc.backend.support.TestData.placaBase;
import static com.optimapc.backend.support.TestData.premontadoCompleto;
import static com.optimapc.backend.support.TestData.procesador;
import static com.optimapc.backend.support.TestData.ram;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.optimapc.backend.catalogo.dto.ComponenteDto;
import com.optimapc.backend.catalogo.dto.PremontadoCatalogoDto;
import com.optimapc.backend.catalogo.dto.ValoracionDto;
import com.optimapc.backend.catalogo.dto.ValoracionRequest;
import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Caja;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.FuenteAlimentacion;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.PerfilUsuario;
import com.optimapc.backend.domain.PlacaBase;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.RefrigeradorCPU;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Usuario;
import com.optimapc.backend.domain.Valoracion;
import com.optimapc.backend.montarPC.RendimientoService;
import com.optimapc.backend.pedido.ItemPedidoRepository;
import com.optimapc.backend.recomendacion.PerfilUsuarioRepository;
import com.optimapc.backend.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PremontadoCatalogoServiceTest {

    @Mock
    private PremontadoRepository premontadoRepository;
    @Mock
    private ValoracionRepository valoracionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    private PremontadoCatalogoService service;

    @BeforeEach
    void setUp() {
        service = new PremontadoCatalogoService(premontadoRepository, valoracionRepository,
                usuarioRepository, new RendimientoService(), perfilUsuarioRepository, itemPedidoRepository);
    }

    private Premontado gamingAsus() {
        return premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING);
    }

    private Premontado ofimaticaHp() {
        return premontadoCompleto(2L, "HP", "Office", null, true, TipoUso.OFIMATICA);
    }

    @Test
    void listarMapeaYNormalizaTodosLosPremontados() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus(), ofimaticaHp()));
        when(itemPedidoRepository.sumComprasPorPremontado()).thenReturn(List.<Object[]>of(new Object[]{1L, 4L}));

        List<PremontadoCatalogoDto> dtos = service.listar();

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(PremontadoCatalogoDto::marca).contains("Asus", "HP");
        PremontadoCatalogoDto asus = dtos.stream().filter(d -> d.id().equals(1L)).findFirst().orElseThrow();
        assertThat(asus.numeroCompras()).isEqualTo(4L);
        assertThat(asus.precioReducido()).isNotNull(); // tiene descuento
        assertThat(asus.componentes()).hasSize(4);
    }

    @Test
    void listarAplicaFiltrosDePrecioMarcaUsoYReacondicionado() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus(), ofimaticaHp()));
        when(itemPedidoRepository.sumComprasPorPremontado()).thenReturn(List.of());

        // Solo gaming, marca Asus, no reacondicionado, precio holgado
        List<PremontadoCatalogoDto> dtos = service.listar(0.0, 5000.0, List.of(TipoUso.GAMING), "asus", false);

        assertThat(dtos).extracting(PremontadoCatalogoDto::marca).containsExactly("Asus");
    }

    @Test
    void obtenerTodosInicializaColecciones() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus()));
        assertThat(service.obtenerTodosLosPremontados()).hasSize(1);
    }

    @Test
    void obtenerPorIdDevuelveDtoSiExiste() {
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(gamingAsus()));
        Optional<PremontadoCatalogoDto> dto = service.obtenerPorId(1L);
        assertThat(dto).isPresent();
        assertThat(dto.get().titulo()).contains("Asus");
    }

    @Test
    void valoracionesDeProductoInexistenteDevuelveListaVacia() {
        when(premontadoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.obtenerValoracionesDelProducto(99L)).isEmpty();
    }

    @Test
    void valoracionesSeDevuelvenOrdenadasPorFecha() {
        Premontado premontado = gamingAsus();
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        premontado.getValoraciones().add(valoracion(premontado, usuario, 4, LocalDateTime.now().minusDays(1)));
        premontado.getValoraciones().add(valoracion(premontado, usuario, 5, LocalDateTime.now()));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(premontado));

        List<ValoracionDto> valoraciones = service.obtenerValoracionesDelProducto(1L);

        assertThat(valoraciones).hasSize(2);
        assertThat(valoraciones.get(0).calificacion()).isEqualTo(5); // la más reciente primero
        assertThat(valoraciones.get(0).usuarioNombre()).isEqualTo("Ana García");
    }

    @Test
    void crearValoracionSinUsuarioLanzaUnauthorized() {
        ValoracionRequest request = new ValoracionRequest(5, "Genial");
        assertThatThrownBy(() -> service.crearValoracion(1L, null, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no autenticado");
    }

    @Test
    void crearValoracionConUsuarioInexistenteLanzaNotFound() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.crearValoracion(1L, 7L, new ValoracionRequest(5, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void crearValoracionDuplicadaLanzaConflict() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(gamingAsus()));
        when(valoracionRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.crearValoracion(1L, 7L, new ValoracionRequest(5, "x")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ya has valorado");
    }

    @Test
    void crearValoracionCorrectaGuardaYActualizaPerfil() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Ana");
        usuario.setApellidos("García");
        Premontado premontado = gamingAsus();
        PerfilUsuario perfil = new PerfilUsuario();
        perfil.setUsuario(usuario);

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(premontado));
        when(valoracionRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        when(valoracionRepository.save(any(Valoracion.class))).thenAnswer(inv -> {
            Valoracion v = inv.getArgument(0);
            v.setId(500L);
            return v;
        });
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.of(perfil));

        ValoracionDto dto = service.crearValoracion(1L, 7L, new ValoracionRequest(5, "  Excelente  "));

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.calificacion()).isEqualTo(5);
        assertThat(dto.comentario()).isEqualTo("Excelente"); // normalizado (trim)
        verify(perfilUsuarioRepository).save(perfil);
    }

    @Test
    void normalizarListaPreparaRendimientoPorEuro() {
        Premontado p = gamingAsus();
        service.normalizarLista(List.of(p));
        assertThat(p.getRendimientoPorEuro()).isNotNull();
    }

    @Test
    void toDtoMapeaEspecificacionesDeTodosLosTiposDeComponente() {
        Premontado p = new Premontado();
        p.setId(10L);
        p.setNombre("Full");
        p.setMarca("Optima");
        p.setEsReacondicionado(false);
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(1);

        TarjetaGrafica gpu = gpu(1L, 12, 2500, 600.0);
        gpu.setLongitud(300);
        gpu.setConsumoWatts(250);
        Procesador cpu = procesador(2L, "AM5", 8, 5.0, 105, 300.0);
        cpu.setConsumoWatts(120);
        MemoriaRAM ram = ram(3L, "DDR5", 2, 16, 6000, 100.0);
        ram.setConsumoWatts(10);
        Almacenamiento sto = almacenamiento(4L, "SSD", "M.2 NVMe", 1000, 90.0);
        sto.setConsumoWatts(8);
        PlacaBase pb = placaBase(5L, "AM5", "DDR5", "ATX", 4, 128, 200.0);
        pb.setConsumoWatts(15);
        FuenteAlimentacion fa = fuente(6L, 750, 110.0);
        fa.setModular("Full");
        fa.setConsumoWatts(0);
        Caja caja = caja(7L, "Torre", 80.0);
        caja.setPanelLateral("Cristal");
        caja.setConsumoWatts(5);
        RefrigeradorCPU refri = new RefrigeradorCPU();
        refri.setId(8L);
        refri.setNombre("Refri");
        refri.setPrecio(60.0);
        refri.setRpmMin(800);
        refri.setRpmMax(1800);
        refri.setNivelRuidoMin(20.0);
        refri.setNivelRuidoMax(35.0);
        refri.setConsumoWatts(6);

        addComponente(p, gpu, "GPU");
        addComponente(p, cpu, "CPU");
        addComponente(p, ram, "RAM");
        addComponente(p, sto, "STORAGE");
        addComponente(p, pb, "PLACA");
        addComponente(p, fa, "FUENTE");
        addComponente(p, caja, "CAJA");
        addComponente(p, refri, "REFRI");

        PremontadoCatalogoDto dto = service.toDto(p);

        assertThat(dto.componentes()).hasSize(8);
        assertThat(dto.componentes()).extracting(ComponenteDto::especificacion)
                .anyMatch(s -> s.contains("núcleos"))
                .anyMatch(s -> s.contains("RPM"))
                .anyMatch(s -> s.contains("Modular"))
                .anyMatch(s -> s.contains("Panel lateral"));
    }

    @Test
    void toDtoConCamposOpcionalesAusentesYNombreEnBlanco() {
        Premontado p = new Premontado();
        p.setId(11L);
        p.setNombre("   "); // en blanco -> el titulo cae a la marca
        p.setMarca("Optima");
        p.setEsReacondicionado(false);
        p.setSistemaOperativo(null); // -> sistemaOperativo null en el dto
        p.setStock(1);

        // GPU sin frecuenciaBoost ni consumo -> formatFrecuencia solo con base
        TarjetaGrafica gpu = new TarjetaGrafica();
        gpu.setId(1L);
        gpu.setNombre("GPU");
        gpu.setPrecio(400.0);
        gpu.setChipset("RTX");
        gpu.setMemoria(8);
        gpu.setFrecuenciaBase(1500);
        // CPU sin consumoWatts pero con tdp -> rama del tdp
        Procesador cpu = procesador(2L, "AM5", 8, 5.0, 95, 250.0); // tdp 95, consumoWatts null
        // Refrigerador con rpmMax null (rango devuelve el minimo) y sin niveles de ruido
        RefrigeradorCPU refri = new RefrigeradorCPU();
        refri.setId(3L);
        refri.setNombre("Refri");
        refri.setPrecio(50.0);
        refri.setRpmMin(1000);

        addComponente(p, gpu, "GPU");
        addComponente(p, cpu, "CPU");
        addComponente(p, refri, "REFRI");

        PremontadoCatalogoDto dto = service.toDto(p);

        assertThat(dto.titulo()).isEqualTo("Optima");
        assertThat(dto.sistemaOperativo()).isNull();
        assertThat(dto.componentes()).hasSize(3);
    }

    @Test
    void listarFiltraPorLimitesDePrecio() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus(), ofimaticaHp()));
        when(itemPedidoRepository.sumComprasPorPremontado()).thenReturn(List.of());

        // gamingAsus: 1170 base con 10% dto -> 1053 efectivo; ofimaticaHp: 1170 sin dto
        // minPrice 1100 deja fuera al Asus (1053 < 1100)
        assertThat(service.listar(1100.0, null, null, null, null))
                .extracting(PremontadoCatalogoDto::marca).containsExactly("HP");
        // maxPrice 1100 deja fuera al HP (1170 > 1100)
        assertThat(service.listar(null, 1100.0, null, null, null))
                .extracting(PremontadoCatalogoDto::marca).containsExactly("Asus");
    }

    @Test
    void crearValoracionConComentarioEnBlancoLoNormalizaANull() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Ana");
        usuario.setApellidos("García");

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(gamingAsus()));
        when(valoracionRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        when(valoracionRepository.save(any(Valoracion.class))).thenAnswer(inv -> inv.getArgument(0));
        // Sin perfil asociado -> la rama ifPresent no se ejecuta
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.empty());

        ValoracionDto dto = service.crearValoracion(1L, 7L, new ValoracionRequest(4, "   "));

        assertThat(dto.comentario()).isNull();
    }

    @Test
    void crearValoracionConComentarioNuloLoMantieneNull() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Ana");
        usuario.setApellidos("García");

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(premontadoRepository.findById(1L)).thenReturn(Optional.of(gamingAsus()));
        when(valoracionRepository.existsByUsuario_IdAndPremontado_Id(7L, 1L)).thenReturn(false);
        when(valoracionRepository.save(any(Valoracion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilUsuarioRepository.findByUsuario_Id(7L)).thenReturn(Optional.empty());

        ValoracionDto dto = service.crearValoracion(1L, 7L, new ValoracionRequest(3, null));

        assertThat(dto.comentario()).isNull();
    }

    @Test
    void listarFiltraPorReacondicionadoYListaDeTiposVacia() {
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(gamingAsus(), ofimaticaHp()));
        when(itemPedidoRepository.sumComprasPorPremontado()).thenReturn(List.of());

        // ofimaticaHp es reacondicionado; reacondicionado=true lo deja solo a él
        assertThat(service.listar(null, null, null, null, true))
                .extracting(PremontadoCatalogoDto::marca).containsExactly("HP");
        // una lista de tipos vacía no filtra
        assertThat(service.listar(null, null, List.of(), null, null)).hasSize(2);
    }

    @Test
    void toDtoConComponentesSinConsumoYRangosParciales() {
        Premontado p = new Premontado();
        p.setId(12L);
        p.setNombre("X");
        p.setMarca("Optima");
        p.setEsReacondicionado(false);
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(1);

        Procesador cpu = procesador(1L, "AM5", 8, 5.0, 95, 300.0); // tdp 95, consumoWatts null
        PlacaBase pb = placaBase(2L, "AM5", "DDR5", "ATX", 4, 128, 200.0); // sin consumo
        FuenteAlimentacion fa = fuente(3L, 750, 90.0); // sin consumo
        Caja caja = caja(4L, "Torre", 80.0); // sin consumo
        RefrigeradorCPU refri = new RefrigeradorCPU();
        refri.setId(5L);
        refri.setNombre("Refri");
        refri.setPrecio(50.0);
        refri.setRpmMax(1500); // rpmMin null -> formatRango con minimo nulo
        refri.setNivelRuidoMin(20.0);
        refri.setNivelRuidoMax(35.0); // ruido presente

        addComponente(p, cpu, "CPU");
        addComponente(p, pb, "PLACA");
        addComponente(p, fa, "FUENTE");
        addComponente(p, caja, "CAJA");
        addComponente(p, refri, "REFRI");

        assertThat(service.toDto(p).componentes()).hasSize(5);
    }

    @Test
    void toDtoConNombreNuloUsaSoloLaMarca() {
        Premontado p = gamingAsus();
        p.setNombre(null);
        assertThat(service.toDto(p).titulo()).isEqualTo("Asus");
    }

    @Test
    void obtenerTodosIgnoraComponentesSinComponenteAsociado() {
        Premontado p = gamingAsus();
        ConfiguracionComponente vacio = new ConfiguracionComponente();
        vacio.setCantidad(1); // componente null
        p.agregarComponente(vacio);
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(p));

        assertThat(service.obtenerTodosLosPremontados()).hasSize(1);
    }

    private void addComponente(Premontado p, Componente c, String categoria) {
        ConfiguracionComponente cc = new ConfiguracionComponente();
        cc.setComponente(c);
        cc.setCategoria(categoria);
        cc.setCantidad(1);
        p.agregarComponente(cc);
    }

    private Valoracion valoracion(Premontado premontado, Usuario usuario, int puntuacion, LocalDateTime fecha) {
        Valoracion v = new Valoracion();
        v.setPuntuacion(puntuacion);
        v.setUsuario(usuario);
        v.setPremontado(premontado);
        v.setFecha(fecha);
        return v;
    }
}

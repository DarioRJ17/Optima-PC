package com.optimapc.backend.support;

import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Caja;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.ConfiguracionPC;
import com.optimapc.backend.domain.FuenteAlimentacion;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.PlacaBase;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoSO;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Valoracion;

import java.util.Arrays;
import java.util.List;

/**
 * Builders de entidades para los tests. Mantienen los tests legibles y evitan
 * repetir la construcción de componentes con muchos campos.
 */
public final class TestData {

    private TestData() {
    }

    public static Procesador procesador(Long id, String socket, int nucleos, double boost, Integer tdp, double precio) {
        Procesador p = new Procesador();
        p.setId(id);
        p.setNombre("CPU-" + id);
        p.setPrecio(precio);
        p.setSocket(socket);
        p.setNucleos(nucleos);
        p.setFrecuenciaBase(boost - 1.0);
        p.setFrecuenciaBoost(boost);
        p.setTdp(tdp);
        return p;
    }

    public static TarjetaGrafica gpu(Long id, int memoriaGb, int boostMhz, double precio) {
        TarjetaGrafica g = new TarjetaGrafica();
        g.setId(id);
        g.setNombre("GPU-" + id);
        g.setChipset("RTX " + id);
        g.setPrecio(precio);
        g.setMemoria(memoriaGb);
        g.setFrecuenciaBase(boostMhz - 200);
        g.setFrecuenciaBoost(boostMhz);
        return g;
    }

    public static MemoriaRAM ram(Long id, String tipoDDR, int modulos, int gbPorModulo, int velocidad, double precio) {
        MemoriaRAM r = new MemoriaRAM();
        r.setId(id);
        r.setNombre("RAM-" + id);
        r.setPrecio(precio);
        r.setTipoDDR(tipoDDR);
        r.setNumModulos(modulos);
        r.setGbPorModulo(gbPorModulo);
        r.setVelocidad(velocidad);
        r.setLatenciaCAS(16);
        return r;
    }

    public static Almacenamiento almacenamiento(Long id, String tipo, String interfaz, int capacidad, double precio) {
        Almacenamiento a = new Almacenamiento();
        a.setId(id);
        a.setNombre("SSD-" + id);
        a.setPrecio(precio);
        a.setTipo(tipo);
        a.setInterfaz(interfaz);
        a.setCapacidad(capacidad);
        a.setFactorForma("M.2");
        return a;
    }

    public static PlacaBase placaBase(Long id, String socket, String tipoDDR, String factorForma,
                                      int ranuras, int memoriaMaxima, double precio) {
        PlacaBase pb = new PlacaBase();
        pb.setId(id);
        pb.setNombre("MOBO-" + id);
        pb.setPrecio(precio);
        pb.setSocket(socket);
        pb.setTipoDDR(tipoDDR);
        pb.setFactorForma(factorForma);
        pb.setRanurasMemoria(ranuras);
        pb.setMemoriaMaxima(memoriaMaxima);
        return pb;
    }

    public static Caja caja(Long id, String tipo, double precio) {
        Caja c = new Caja();
        c.setId(id);
        c.setNombre("CAJA-" + id);
        c.setPrecio(precio);
        c.setTipo(tipo);
        return c;
    }

    public static FuenteAlimentacion fuente(Long id, int potencia, double precio) {
        FuenteAlimentacion f = new FuenteAlimentacion();
        f.setId(id);
        f.setNombre("PSU-" + id);
        f.setPrecio(precio);
        f.setPotencia(potencia);
        f.setTipo("ATX");
        return f;
    }

    /** Construye una ConfiguracionPC con los componentes dados, cada uno con cantidad 1. */
    public static ConfiguracionPC config(Componente... componentes) {
        ConfiguracionPC config = new ConfiguracionPC();
        for (Componente c : componentes) {
            ConfiguracionComponente cc = new ConfiguracionComponente();
            cc.setComponente(c);
            cc.setCantidad(1);
            config.agregarComponente(cc);
        }
        return config;
    }

    public static Premontado premontado(Long id, double precio, Boolean reacondicionado, TipoUso... usos) {
        Premontado p = new Premontado();
        p.setId(id);
        p.setNombre("PRE-" + id);
        p.setMarca("OptimaPC");
        p.setEsReacondicionado(reacondicionado);
        p.setSistemaOperativo(TipoSO.WINDOWS);
        // El precio se calcula desde los componentes: añadimos un componente "ficticio" con ese precio.
        Almacenamiento dummy = almacenamiento(900 + id, "SSD", "M.2", 1000, precio);
        ConfiguracionComponente cc = new ConfiguracionComponente();
        cc.setComponente(dummy);
        cc.setCantidad(1);
        p.agregarComponente(cc);
        p.getUsosPrevistos().addAll(Arrays.asList(usos));
        return p;
    }

    /** Premontado con CPU, GPU, RAM y almacenamiento reales (precio base 1170€). */
    public static Premontado premontadoCompleto(Long id, String marca, String nombre,
                                                Integer descuento, boolean reacondicionado, TipoUso... usos) {
        Premontado p = new Premontado();
        p.setId(id);
        p.setNombre(nombre);
        p.setMarca(marca);
        p.setDescripcion("Equipo de prueba");
        p.setEsReacondicionado(reacondicionado);
        p.setSistemaOperativo(TipoSO.WINDOWS);
        p.setStock(5);
        p.setDescuento(descuento);
        addComp(p, procesador(100 + id, "AM5", 16, 5.5, 105, 300.0), "CPU");
        addComp(p, gpu(200 + id, 12, 2500, 600.0), "GPU");
        addComp(p, ram(300 + id, "DDR5", 2, 16, 6000, 150.0), "RAM");
        addComp(p, almacenamiento(400 + id, "SSD", "M.2 NVMe", 2000, 120.0), "STORAGE");
        p.getUsosPrevistos().addAll(Arrays.asList(usos));
        return p;
    }

    private static void addComp(Premontado p, Componente c, String categoria) {
        ConfiguracionComponente cc = new ConfiguracionComponente();
        cc.setComponente(c);
        cc.setCategoria(categoria);
        cc.setCantidad(1);
        p.agregarComponente(cc);
    }

    public static Valoracion valoracion(int puntuacion) {
        Valoracion v = new Valoracion();
        v.setPuntuacion(puntuacion);
        return v;
    }

    public static List<Valoracion> valoraciones(int... puntuaciones) {
        return Arrays.stream(puntuaciones).mapToObj(TestData::valoracion).toList();
    }
}

package com.optimapc.backend.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.optimapc.backend.modelo.Almacenamiento;
import com.optimapc.backend.modelo.Caja;
import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.ConfiguracionComponente;
import com.optimapc.backend.modelo.ConfiguracionPC;
import com.optimapc.backend.modelo.FuenteAlimentacion;
import com.optimapc.backend.modelo.MemoriaRAM;
import com.optimapc.backend.modelo.PerfilUsuario;
import com.optimapc.backend.modelo.PlacaBase;
import com.optimapc.backend.modelo.Procesador;
import com.optimapc.backend.modelo.Premontado;
import com.optimapc.backend.modelo.RefrigeradorCPU;
import com.optimapc.backend.modelo.TarjetaGrafica;
import com.optimapc.backend.modelo.TipoSO;
import com.optimapc.backend.modelo.TipoUso;
import com.optimapc.backend.modelo.Valoracion;
import com.optimapc.backend.usuario.Usuario;

@Component
public class CsvDataLoader implements ApplicationRunner {

	@PersistenceContext
	private EntityManager entityManager;

	private final PasswordEncoder passwordEncoder;

	public CsvDataLoader(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		importarComponentesSiNecesario();
		sembrarUsuariosSiNecesario();
		sembrarConfiguracionesSiNecesario();
		sembrarPremontadosSiNecesario();
		sembrarValoracionesSiNecesario();
	}

	private void importarComponentesSiNecesario() throws IOException {
		if (tieneFilas(Componente.class)) {
			return;
		}

		importarProcesadores();
		importarTarjetasGraficas();
		importarPlacasBase();
		importarMemoriasRam();
		importarAlmacenamientos();
		importarCajas();
		importarFuentesAlimentacion();
		importarRefrigeradoresCpu();
	}

	private void sembrarUsuariosSiNecesario() {
		asegurarUsuario("ana.romero@optimapc.local", "Ana", "Romero", "AnaRomero123!");
		asegurarUsuario("carlos.martin@optimapc.local", "Carlos", "Martín", "CarlosMartin123!");
		asegurarUsuario("laura.gomez@optimapc.local", "Laura", "Gómez", "LauraGomez123!");
		asegurarUsuario("diego.perez@optimapc.local", "Diego", "Pérez", "DiegoPerez123!");
		asegurarUsuario("nuria.santos@optimapc.local", "Nuria", "Santos", "NuriaSantos123!");
	}

	private void sembrarConfiguracionesSiNecesario() {
		if (tieneConfiguracionesGenericas()) {
			return;
		}

		Procesador procesador = primer(Procesador.class);
		TarjetaGrafica tarjetaGrafica = primer(TarjetaGrafica.class);
		PlacaBase placaBase = primer(PlacaBase.class);
		MemoriaRAM memoriaRAM = primer(MemoriaRAM.class);
		Almacenamiento almacenamiento = primer(Almacenamiento.class);
		FuenteAlimentacion fuenteAlimentacion = primer(FuenteAlimentacion.class);
		Caja caja = primer(Caja.class);
		RefrigeradorCPU refrigeradorCPU = primer(RefrigeradorCPU.class);

		ConfiguracionPC gaming = new ConfiguracionPC();
		gaming.setUsosPrevistos(Set.of(TipoUso.GAMING));
		gaming.setFavorita(Boolean.TRUE);
		agregarComponente(gaming, "CPU", procesador, 1);
		agregarComponente(gaming, "GPU", tarjetaGrafica, 1);
		agregarComponente(gaming, "Placa base", placaBase, 1);
		agregarComponente(gaming, "RAM", memoriaRAM, 2);
		agregarComponente(gaming, "Almacenamiento", almacenamiento, 1);
		agregarComponente(gaming, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(gaming, "Caja", caja, 1);
		agregarComponente(gaming, "Refrigeración", refrigeradorCPU, 1);
		entityManager.persist(gaming);

		ConfiguracionPC ofimatica = new ConfiguracionPC();
		ofimatica.setUsosPrevistos(Set.of(TipoUso.OFIMATICA));
		ofimatica.setFavorita(Boolean.FALSE);
		agregarComponente(ofimatica, "CPU", procesador, 1);
		agregarComponente(ofimatica, "Placa base", placaBase, 1);
		agregarComponente(ofimatica, "RAM", memoriaRAM, 1);
		agregarComponente(ofimatica, "Almacenamiento", almacenamiento, 1);
		agregarComponente(ofimatica, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(ofimatica, "Caja", caja, 1);
		entityManager.persist(ofimatica);

		ConfiguracionPC edicion = new ConfiguracionPC();
		edicion.setUsosPrevistos(Set.of(TipoUso.EDICION));
		edicion.setFavorita(Boolean.FALSE);
		agregarComponente(edicion, "CPU", procesador, 1);
		agregarComponente(edicion, "GPU", tarjetaGrafica, 1);
		agregarComponente(edicion, "Placa base", placaBase, 1);
		agregarComponente(edicion, "RAM", memoriaRAM, 2);
		agregarComponente(edicion, "Almacenamiento", almacenamiento, 2);
		agregarComponente(edicion, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(edicion, "Caja", caja, 1);
		entityManager.persist(edicion);
	}

	private void sembrarPremontadosSiNecesario() {
		if (tieneFilas(Premontado.class)) return;
		// OFIMÁTICA
		asegurarPremontadoOfimaticaBasica();
		asegurarPremontadoOfimaticaProductiva();
		asegurarPremontadoOfimaticaAM5();
		asegurarPremontadoOficinaProfesional();
		// PROGRAMACIÓN
		asegurarPremontadoProgramacionWeb();
		asegurarPremontadoProgramacionFullStack();
		asegurarPremontadoDevOps();
		asegurarPremontadoDataScience();
		// GAMING
		asegurarPremontadoGamingEntrada1080p();
		asegurarPremontadoGamingEconomico();
		asegurarPremontadoGamingEquilibrado1080p();
		asegurarPremontadoGamingAlto1080p();
		asegurarPremontadoGaming1440pEntrada();
		asegurarPremontadoGaming1440p();
		asegurarPremontadoGaming1440pAlto();
		asegurarPremontadoGaming4K();
		asegurarPremontadoGamingExtremo();
		asegurarPremontadoGamingYStreaming();
		// EDICIÓN
		asegurarPremontadoEdicionFotografica();
		asegurarPremontadoEdicionVideo1080p();
		asegurarPremontadoEdicionVideo4K();
		asegurarPremontadoEdicionProfesional();
		asegurarPremontadoMotionGraphics3D();
		// STREAMING
		asegurarPremontadoStreamingEconomico();
		asegurarPremontadoStreamingProfesional();
		asegurarPremontadoCreadorContenido();
		// VERSÁTILES
		asegurarPremontadoTodoEnUnoVersatil();
		asegurarPremontadoWorkstationVersatil();
	}

	// ── OFIMÁTICA ────────────────────────────────────────────────────────────

	private void asegurarPremontadoOfimaticaBasica() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && c.getGraficaIntegrada() != null && !c.getGraficaIntegrada().isBlank() && c.getNucleos() != null && c.getNucleos() >= 6);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()) && p.getFactorForma() != null && p.getFactorForma().toUpperCase().contains("MICRO"));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 8);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() <= 500);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 500);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("MICROATX"));
		Premontado p = new Premontado();
		p.setNombre("Ofimática básica");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Equipo de entrada para navegación, correo, Office y videollamadas. CPU con gráfica integrada; no necesita GPU dedicada.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.LINUX);
		p.setStock(15);
		p.setEsReacondicionado(Boolean.TRUE);
		p.setUsosPrevistos(Set.of(TipoUso.OFIMATICA));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoOfimaticaProductiva() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && c.getGraficaIntegrada() != null && !c.getGraficaIntegrada().isBlank() && c.getNucleos() != null && c.getNucleos() >= 8);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Ofimática productiva");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("APU de 8 núcleos y 32 GB de RAM para multitarea fluida, hojas de cálculo complejas y reuniones online simultáneas sin GPU dedicada.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(12);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.OFIMATICA));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoOfimaticaAM5() {
		// APU Zen 4 de nueva generación: el campo graphics contiene el modelo (ej. "Radeon 740M"), no solo "Radeon"
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getGraficaIntegrada() != null && c.getGraficaIntegrada().length() > "Radeon".length());
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()) && p.getFactorForma() != null && p.getFactorForma().toUpperCase().contains("MICRO"));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("MICROATX"));
		Premontado p = new Premontado();
		p.setNombre("Ofimática AM5");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("APU Zen 4 en plataforma AM5 con DDR5. Gráfica integrada RDNA3 capaz de 4K, corrección de fotos básica y soporte multi-monitor sin GPU discreta.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(10);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.OFIMATICA, TipoUso.PROGRAMACION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoOficinaProfesional() {
		// APU AM5 de alta gama (Radeon 780M): precio >= 250
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getGraficaIntegrada() != null && c.getGraficaIntegrada().length() > "Radeon".length() && c.getPrecio() != null && c.getPrecio() >= 250.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 650);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Oficina profesional");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Workstation de oficina con APU Radeon 780M, 32 GB DDR5 y 2 TB. Ideal para PYMES, soporte multi-monitor y aplicaciones ERP sin GPU dedicada.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(8);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.OFIMATICA, TipoUso.PROGRAMACION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	// ── PROGRAMACIÓN ─────────────────────────────────────────────────────────

	private void asegurarPremontadoProgramacionWeb() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && c.getGraficaIntegrada() != null && !c.getGraficaIntegrada().isBlank() && c.getNucleos() != null && c.getNucleos() >= 6);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()) && p.getFactorForma() != null && p.getFactorForma().toUpperCase().contains("MICRO"));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 8);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 500);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("MICROATX"));
		Premontado p = new Premontado();
		p.setNombre("Programación web");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Equipo compacto y silencioso para desarrollo frontend/backend. APU con 16 GB de RAM para el IDE, el navegador y un servidor local simultáneamente.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.LINUX);
		p.setStock(10);
		p.setEsReacondicionado(Boolean.TRUE);
		p.setUsosPrevistos(Set.of(TipoUso.PROGRAMACION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoProgramacionFullStack() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && c.getGraficaIntegrada() != null && !c.getGraficaIntegrada().isBlank() && c.getNucleos() != null && c.getNucleos() >= 8);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Programación full stack");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("32 GB de RAM y 2 TB SSD para varios proyectos, bases de datos locales, máquinas virtuales ligeras e IDEs exigentes sin compromisos.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.LINUX);
		p.setStock(8);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.PROGRAMACION, TipoUso.OFIMATICA));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoDevOps() {
		// 12 núcleos AM4 sin iGPU para compilaciones y contenedores + GPU de display básica
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 12);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() <= 130.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 650);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> r.getPrecio() != null && r.getPrecio() >= 25.0);
		Premontado p = new Premontado();
		p.setNombre("DevOps y contenedores");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("12 núcleos Zen 3 para compilaciones paralelas, stacks Docker y pipelines CI/CD locales. 32 GB RAM y 2 TB SSD para imágenes y volúmenes grandes.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.SIN_SO);
		p.setStock(6);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.PROGRAMACION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoDataScience() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 300.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 380.0 && g.getPrecio() <= 540.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 750);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(240).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Data science");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Plataforma AM5 de 12 núcleos con GPU CUDA para acelerar notebooks de machine learning. 32 GB DDR5 y 2 TB NVMe para datasets locales y entornos conda.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.LINUX);
		p.setStock(5);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.PROGRAMACION, TipoUso.EDICION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	// ── GAMING ───────────────────────────────────────────────────────────────

	private void asegurarPremontadoGamingEntrada1080p() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 6);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 220.0 && g.getPrecio() <= 285.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 8);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Gaming entrada 1080p");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Primera PC gaming a precio mínimo. GPU de entrada para jugar títulos competitivos en 1080p a más de 60 FPS con ajustes medios-altos.");
		p.setMarca("OptimaPC");
		p.setDescuento(10);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(12);
		p.setEsReacondicionado(Boolean.TRUE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGamingEconomico() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 6 && c.getPrecio() != null && c.getPrecio() >= 140.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 250.0 && g.getPrecio() <= 320.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 8);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Gaming económico");
		p.setFavorita(Boolean.TRUE);
		p.setDescripcion("Equipo gaming por debajo de los 900 €. GPU de generación actual para 1080p a 60+ FPS en títulos AAA con ajustes altos. Excelente relación calidad-precio.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.SIN_SO);
		p.setStock(10);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGamingEquilibrado1080p() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 6 && c.getPrecio() != null && c.getPrecio() >= 150.0 && c.getPrecio() <= 230.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 360.0 && g.getPrecio() <= 520.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 650);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Gaming equilibrado 1080p");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Plataforma AM5 moderna con GPU de gama media para 1080p máximos o 1440p en ajustes medios. 32 GB DDR5 y amplia vida útil gracias al socket AM5.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(8);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGamingAlto1080p() {
		// Ryzen 7 5700X3D: AM4, 8 núcleos, ~313 €
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 8 && c.getPrecio() != null && c.getPrecio() >= 290.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 420.0 && g.getPrecio() <= 650.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 750);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> r.getPrecio() != null && r.getPrecio() >= 30.0 && r.getTamano() == null);
		Premontado p = new Premontado();
		p.setNombre("Gaming alto 1080p");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("CPU con caché 3D para máximo FPS en juegos competitivos 1080p. GPU de gama media sólida que supera los 100 FPS en todos los títulos AAA actuales.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(7);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGaming1440pEntrada() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 8 && c.getPrecio() != null && c.getPrecio() >= 220.0 && c.getPrecio() <= 310.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 550.0 && g.getPrecio() <= 780.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 750);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> r.getPrecio() != null && r.getPrecio() >= 30.0 && r.getTamano() == null);
		Premontado p = new Premontado();
		p.setNombre("Gaming 1440p entrada");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("El salto a 1440p con GPU capaz de superar 60 FPS en títulos exigentes. Plataforma AM5 de 8 núcleos con DDR5 preparada para los próximos años.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(8);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGaming1440p() {
		// Ryzen 7 7800X3D: AM5, 8 núcleos, ~340 €
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 8 && c.getPrecio() != null && c.getPrecio() >= 310.0 && c.getPrecio() <= 400.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 700.0 && g.getPrecio() <= 1060.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 850);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(240).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Gaming 1440p");
		p.setFavorita(Boolean.TRUE);
		p.setDescripcion("El mejor punto dulce gaming del catálogo. CPU con caché 3D líder en FPS + GPU de gama alta para 1440p a más de 100 FPS en todos los juegos actuales con ajustes máximos.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(6);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGaming1440pAlto() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 330.0 && c.getPrecio() <= 430.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 950.0 && g.getPrecio() <= 1450.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 850);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(360).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Gaming 1440p alto");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Potencia de sobra para 1440p a alta tasa de refresco o 4K casual. CPU de 12 núcleos con margen para streaming simultáneo y GPU de primera línea.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(5);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGaming4K() {
		// Ryzen 7 9800X3D: AM5, 8 núcleos, ~452 €
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() == 8 && c.getPrecio() != null && c.getPrecio() >= 430.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 1300.0 && g.getPrecio() <= 2100.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 1000);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(360).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Gaming 4K");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("La CPU gaming más rápida del mercado combinada con una GPU de élite. Juega en 4K a ajustes máximos o en 1440p a más de 165 FPS sin ninguna concesión.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(3);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGamingExtremo() {
		// Ryzen 9 9950X3D: AM5, 16 núcleos, ~650 €
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 16 && c.getPrecio() != null && c.getPrecio() >= 600.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 2500.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 32);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 1000);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(360).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Gaming extremo");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Sin compromisos: 16 núcleos con 3D V-Cache y la GPU más potente del mercado. Diseñado para 4K a 120+ FPS en cualquier título presente y futuro.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.SIN_SO);
		p.setStock(2);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoGamingYStreaming() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 8 && c.getPrecio() != null && c.getPrecio() >= 130.0 && c.getPrecio() <= 200.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 400.0 && g.getPrecio() <= 570.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		Almacenamiento hdd = componenteMasBarato(Almacenamiento.class,
				s -> !"SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 750);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Gaming y streaming");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("8 núcleos para jugar y encodear en OBS sin caídas de FPS. NVENC/AMF de GPU actual para streams 1080p60. HDD de 2 TB para almacenar grabaciones largas.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(7);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.GAMING, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Almacenamiento", hdd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	// ── EDICIÓN ──────────────────────────────────────────────────────────────

	private void asegurarPremontadoEdicionFotografica() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 8 && c.getPrecio() != null && c.getPrecio() >= 130.0 && c.getPrecio() <= 200.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 250.0 && g.getPrecio() <= 400.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 650);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Edición fotográfica");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("32 GB RAM y 2 TB NVMe para catálogos RAW en Lightroom y retoques en Photoshop sin tiempos de espera. GPU con aceleración para filtros y exportación.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(8);
		p.setEsReacondicionado(Boolean.TRUE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoEdicionVideo1080p() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 230.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 400.0 && g.getPrecio() <= 650.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 750);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> r.getPrecio() != null && r.getPrecio() >= 30.0 && r.getTamano() == null);
		Premontado p = new Premontado();
		p.setNombre("Edición vídeo 1080p");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("12 núcleos para renderizar líneas de tiempo complejas en Premiere o Resolve. GPU acelera efectos y exportación H.265. Ideal para youtubers y videógrafos.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.SIN_SO);
		p.setStock(6);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoEdicionVideo4K() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 300.0 && c.getPrecio() <= 380.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 600.0 && g.getPrecio() <= 1060.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		Almacenamiento hdd = componenteMasBarato(Almacenamiento.class,
				s -> !"SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 850);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(360).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Edición vídeo 4K");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Edita vídeo 4K RAW sin proxies. 12 núcleos AM5 + GPU de gama alta + HDD de archivo para los proyectos terminados. Refrigeración AIO para sostenibilidad en renders largos.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(4);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Almacenamiento", hdd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoEdicionProfesional() {
		// Ryzen 9 9950X: AM5, 16 núcleos, ~534 €
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 16 && c.getPrecio() != null && c.getPrecio() >= 500.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 950.0 && g.getPrecio() <= 1450.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 32);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 1000);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(360).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Edición profesional 4K");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Workstation de alto nivel: 16 núcleos Zen 5 y 64 GB DDR5 para proyectos 4K/8K, composición multicapa y renders nocturnos en DaVinci Resolve Studio.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(3);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoMotionGraphics3D() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 16 && c.getPrecio() != null && c.getPrecio() >= 500.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 1300.0 && g.getPrecio() <= 2100.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 32);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 1000);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(360).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Motion graphics y 3D");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Render GPU de escenas 3D en Blender o motion graphics en After Effects acelerados por hardware. 64 GB RAM y GPU top de gama para tiempos de render mínimos.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(2);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	// ── STREAMING ────────────────────────────────────────────────────────────

	private void asegurarPremontadoStreamingEconomico() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 8 && c.getPrecio() != null && c.getPrecio() >= 130.0 && c.getPrecio() <= 200.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 350.0 && g.getPrecio() <= 560.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		Almacenamiento hdd = componenteMasBarato(Almacenamiento.class,
				s -> !"SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 750);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Streaming económico");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("8 núcleos con margen para OBS + juego simultáneo. NVENC/AMF de GPU actual para streams 1080p60 sin impacto en FPS. HDD 2 TB para guardar VODs.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(7);
		p.setEsReacondicionado(Boolean.TRUE);
		p.setUsosPrevistos(Set.of(TipoUso.STREAMING, TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Almacenamiento", hdd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoStreamingProfesional() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 330.0 && c.getPrecio() <= 430.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 750.0 && g.getPrecio() <= 1100.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 850);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(240).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Streaming profesional");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("12 núcleos AM5 para streams 1440p con bitrate alto, editar los VODs y jugar sin cuellos de botella. GPU de élite con el mejor encoder de nueva generación.");
		p.setMarca("OptimaPC");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.SIN_SO);
		p.setStock(4);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.STREAMING, TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoCreadorContenido() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 300.0 && c.getPrecio() <= 370.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 500.0 && g.getPrecio() <= 800.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 850);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(240).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Creador de contenido");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Equilibrio perfecto entre edición y streaming: 12 núcleos para renderizar vídeos mientras se emite en directo, GPU con buen encoder y 2 TB para proyectos.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(5);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION, TipoUso.STREAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	// ── VERSÁTILES ───────────────────────────────────────────────────────────

	private void asegurarPremontadoTodoEnUnoVersatil() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM4".equals(c.getSocket()) && (c.getGraficaIntegrada() == null || c.getGraficaIntegrada().isBlank()) && c.getNucleos() != null && c.getNucleos() >= 6 && c.getPrecio() != null && c.getPrecio() >= 100.0 && c.getPrecio() <= 175.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 300.0 && g.getPrecio() <= 500.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM4".equals(p.getSocket()) && "DDR4".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR4".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 16);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 1000 && s.getCapacidad() < 2000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 650);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		Premontado p = new Premontado();
		p.setNombre("Todo en uno versátil");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("El equipo para quien lo quiere todo: trabaja, programa, edita fotos y juega títulos de gama media. 32 GB de RAM para no cerrar nada nunca.");
		p.setMarca("OptimaPC");
		p.setDescuento(5);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(9);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.OFIMATICA, TipoUso.PROGRAMACION, TipoUso.GAMING));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		entityManager.persist(p);
	}

	private void asegurarPremontadoWorkstationVersatil() {
		Procesador cpu = componenteMasBarato(Procesador.class,
				c -> "AM5".equals(c.getSocket()) && c.getNucleos() != null && c.getNucleos() >= 12 && c.getPrecio() != null && c.getPrecio() >= 300.0 && c.getPrecio() <= 380.0);
		TarjetaGrafica gpu = componenteMasBarato(TarjetaGrafica.class,
				g -> g.getPrecio() != null && g.getPrecio() >= 600.0 && g.getPrecio() <= 1000.0);
		PlacaBase placa = componenteMasBarato(PlacaBase.class,
				p -> "AM5".equals(p.getSocket()) && "DDR5".equalsIgnoreCase(p.getTipoDDR()));
		MemoriaRAM ram = componenteMasBarato(MemoriaRAM.class,
				r -> "DDR5".equalsIgnoreCase(r.getTipoDDR()) && r.getNumModulos() != null && r.getNumModulos() == 2 && r.getGbPorModulo() != null && r.getGbPorModulo() == 32);
		Almacenamiento ssd = componenteMasBarato(Almacenamiento.class,
				s -> "SSD".equalsIgnoreCase(s.getTipo()) && s.getCapacidad() != null && s.getCapacidad() >= 2000 && s.getCapacidad() < 4000);
		FuenteAlimentacion psu = componenteMasBarato(FuenteAlimentacion.class,
				f -> f.getPotencia() != null && f.getPotencia() >= 850);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));
		RefrigeradorCPU cooler = componenteMasBarato(RefrigeradorCPU.class,
				r -> Integer.valueOf(240).equals(r.getTamano()));
		Premontado p = new Premontado();
		p.setNombre("Workstation versátil");
		p.setFavorita(Boolean.FALSE);
		p.setDescripcion("Plataforma AM5 de 12 núcleos con 64 GB DDR5 para exigencias profesionales variadas: compilación, modelos ML ligeros, edición y gaming ocasional en una sola máquina.");
		p.setMarca("OptimaWork");
		p.setDescuento(0);
		p.setSistemaOperativo(TipoSO.WINDOWS);
		p.setStock(4);
		p.setEsReacondicionado(Boolean.FALSE);
		p.setUsosPrevistos(Set.of(TipoUso.EDICION, TipoUso.PROGRAMACION));
		agregarComponente(p, "CPU", cpu, 1);
		agregarComponente(p, "GPU", gpu, 1);
		agregarComponente(p, "Placa base", placa, 1);
		agregarComponente(p, "RAM", ram, 1);
		agregarComponente(p, "Almacenamiento", ssd, 1);
		agregarComponente(p, "Fuente", psu, 1);
		agregarComponente(p, "Caja", caja, 1);
		agregarComponente(p, "Refrigeración", cooler, 1);
		entityManager.persist(p);
	}

	private void sembrarValoracionesSiNecesario() {
		List<Usuario> usuarios = listar(Usuario.class);
		List<Premontado> premontados = listar(Premontado.class);
		if (usuarios.size() < 3 || premontados.size() < 1) {
			return;
		}

		// Añadir valoraciones para premontados que no tengan
		int userCount = usuarios.size();
		for (int i = 0; i < premontados.size(); i++) {
			Premontado p = premontados.get(i);
			if (p.getValoraciones() != null && !p.getValoraciones().isEmpty()) {
				continue;
			}

			int numValoraciones = 1 + (i % 3); // entre 1 y 3 valoraciones
			for (int v = 0; v < numValoraciones; v++) {
				Usuario u = usuarios.get((i + v) % userCount);
				int puntuacion = 3 + ((i + v) % 3); // 3,4,5
				String comentario = switch (puntuacion) {
					case 5 -> "Excelente, muy contento con el rendimiento.";
					case 4 -> "Buen equipo, cumple las expectativas.";
					default -> "Cumple lo básico, relación calidad-precio aceptable.";
				};
				LocalDateTime fecha = LocalDateTime.now().minusDays((i + v) % 60);
				crearValoracion(u, p, puntuacion, comentario, fecha);
			}
		}
	}

	private void persistirUsuario(String email, String nombre, String apellidos, String passwordPlano) {
		Usuario usuario = new Usuario();
		usuario.setEmail(email);
		usuario.setNombre(nombre);
		usuario.setApellidos(apellidos);
		usuario.setPassword(passwordEncoder.encode(passwordPlano));
		entityManager.persist(usuario);

		// Perfil vacío con scores a 0 (vía @PrePersist), igual que al pulsar
		// "Omitir por ahora" en la encuesta. Sin él, las acciones de compra,
		// valoración y favorito usan findByUsuario_Id(...).ifPresent(...) y no
		// tendrían perfil que actualizar para los usuarios del seeder.
		PerfilUsuario perfil = new PerfilUsuario();
		perfil.setUsuario(usuario);
		entityManager.persist(perfil);
	}

	private void asegurarUsuario(String email, String nombre, String apellidos, String passwordPlano) {
		if (buscarUsuarioPorEmail(email) != null) {
			return;
		}

		persistirUsuario(email, nombre, apellidos, passwordPlano);
	}

	private Usuario buscarUsuarioPorEmail(String email) {
		List<Usuario> usuarios = entityManager.createQuery(
				"select u from Usuario u where lower(u.email) = lower(:email)",
				Usuario.class)
				.setParameter("email", email)
				.getResultList();
		return usuarios.isEmpty() ? null : usuarios.get(0);
	}

	private void crearValoracion(Usuario usuario, Premontado premontado, int puntuacion, String comentario, LocalDateTime fecha) {
		Valoracion valoracion = new Valoracion();
		valoracion.setUsuario(usuario);
		valoracion.setPremontado(premontado);
		valoracion.setPuntuacion(puntuacion);
		valoracion.setComentario(comentario);
		valoracion.setFecha(fecha);
		usuario.getValoraciones().add(valoracion);
		premontado.getValoraciones().add(valoracion);
		entityManager.persist(valoracion);
	}

	// private List<Valoracion> listarValoraciones(Premontado premontado) {
	// 	return entityManager.createQuery(
	// 			"select v from Valoracion v where v.premontado = :premontado order by v.id",
	// 			Valoracion.class)
	// 			.setParameter("premontado", premontado)
	// 			.getResultList();
	// }

	private boolean tieneConfiguracionesGenericas() {
		Long total = entityManager.createQuery(
				"select count(c) from ConfiguracionPC c where type(c) = ConfiguracionPC",
				Long.class)
				.getSingleResult();
		return total != null && total > 0;
	}

	private boolean tieneFilas(Class<?> entityClass) {
		Long total = entityManager.createQuery("select count(e) from " + entityClass.getSimpleName() + " e", Long.class)
				.getSingleResult();
		return total != null && total > 0;
	}

	private <T> List<T> listar(Class<T> entityClass) {
		return entityManager.createQuery("select e from " + entityClass.getSimpleName() + " e order by e.id", entityClass)
				.getResultList();
	}

	private <T> T primer(Class<T> entityClass) {
		return listar(entityClass).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No hay datos semilla para " + entityClass.getSimpleName()));
	}

	private <T extends Componente> T componenteMasBarato(Class<T> entityClass, Predicate<T> filtro) {
		return listar(entityClass).stream()
				.filter(filtro)
				.min(Comparator.comparingDouble(Componente::getPrecio))
				.orElseThrow(() -> new IllegalStateException("No hay componentes compatibles para " + entityClass.getSimpleName()));
	}

	private void agregarComponente(ConfiguracionPC configuracion, String categoria, Componente componente, int cantidad) {
		ConfiguracionComponente configuracionComponente = new ConfiguracionComponente();
		configuracionComponente.setCategoria(categoria);
		configuracionComponente.setCantidad(cantidad);
		configuracionComponente.asociarComponente(componente);
		configuracion.agregarComponente(configuracionComponente);
	}

	private void importarProcesadores() throws IOException {
		for (CSVRecord record : leerCsv("data/cpu.csv")) {
			Procesador procesador = new Procesador();
			mapearBase(procesador, record);
			procesador.setNucleos(parseInteger(record, "core_count"));
			procesador.setFrecuenciaBase(parseDouble(record, "core_clock"));
			procesador.setFrecuenciaBoost(parseDouble(record, "boost_clock"));
			procesador.setMicroarquitectura(texto(record, "microarchitecture"));
			procesador.setTdp(parseInteger(record, "tdp"));
			procesador.setGraficaIntegrada(texto(record, "graphics"));
			procesador.setSocket(texto(record, "socket"));
			// Ensure consumoWatts is populated: prefer explicit CSV `consumo`, otherwise fall back to TDP
			if (procesador.getConsumoWatts() == null && procesador.getTdp() != null) {
				procesador.setConsumoWatts(procesador.getTdp());
			}
			entityManager.persist(procesador);
		}
	}

	private void importarTarjetasGraficas() throws IOException {
		for (CSVRecord record : leerCsv("data/video-card.csv")) {
			TarjetaGrafica tarjetaGrafica = new TarjetaGrafica();
			mapearBase(tarjetaGrafica, record);
			tarjetaGrafica.setChipset(texto(record, "chipset"));
			tarjetaGrafica.setMemoria(parseInteger(record, "memory"));
			tarjetaGrafica.setFrecuenciaBase(parseInteger(record, "core_clock"));
			tarjetaGrafica.setFrecuenciaBoost(parseInteger(record, "boost_clock"));
			tarjetaGrafica.setColor(texto(record, "color"));
			tarjetaGrafica.setLongitud(parseInteger(record, "length"));
			entityManager.persist(tarjetaGrafica);
		}
	}

	private void importarPlacasBase() throws IOException {
		for (CSVRecord record : leerCsv("data/motherboard.csv")) {
			PlacaBase placaBase = new PlacaBase();
			mapearBase(placaBase, record);
			placaBase.setSocket(texto(record, "socket"));
			placaBase.setFactorForma(texto(record, "form_factor"));
			placaBase.setMemoriaMaxima(parseInteger(record, "max_memory"));
			placaBase.setRanurasMemoria(parseInteger(record, "memory_slots"));
			placaBase.setColor(texto(record, "color"));
			placaBase.setTipoDDR(texto(record, "ddr_type"));
			entityManager.persist(placaBase);
		}
	}

	private void importarMemoriasRam() throws IOException {
		for (CSVRecord record : leerCsv("data/memory.csv")) {
			MemoriaRAM memoriaRAM = new MemoriaRAM();
			mapearBase(memoriaRAM, record);
			String velocidadStr = texto(record, "speed");
			memoriaRAM.setTipoDDR("DDR" + velocidadStr.split(",")[0].trim());
			memoriaRAM.setVelocidad(Integer.parseInt(velocidadStr.split(",")[1].trim()));
			String modulosStr = texto(record, "modules");
			memoriaRAM.setNumModulos(Integer.parseInt(modulosStr.split(",")[0].trim()));
			memoriaRAM.setGbPorModulo(Integer.parseInt(modulosStr.split(",")[1].trim()));
			memoriaRAM.setColor(texto(record, "color"));
			memoriaRAM.setLatenciaCAS(parseInteger(record, "cas_latency"));
			entityManager.persist(memoriaRAM);
		}
	}

	private void importarAlmacenamientos() throws IOException {
		for (CSVRecord record : leerCsv("data/internal-hard-drive.csv")) {
			Almacenamiento almacenamiento = new Almacenamiento();
			mapearBase(almacenamiento, record);
			almacenamiento.setCapacidad(parseInteger(record, "capacity"));
			almacenamiento.setPrecioPorGB(parseDouble(record, "price_per_gb"));
			almacenamiento.setTipo(texto(record, "type"));
			almacenamiento.setFactorForma(texto(record, "form_factor"));
			almacenamiento.setInterfaz(texto(record, "interface"));
			entityManager.persist(almacenamiento);
		}
	}

	private void importarCajas() throws IOException {
		for (CSVRecord record : leerCsv("data/case.csv")) {
			Caja caja = new Caja();
			mapearBase(caja, record);
			caja.setTipo(texto(record, "type"));
			caja.setColor(texto(record, "color"));
			caja.setPanelLateral(texto(record, "side_panel"));
			entityManager.persist(caja);
		}
	}

	private void importarFuentesAlimentacion() throws IOException {
		for (CSVRecord record : leerCsv("data/power-supply.csv")) {
			FuenteAlimentacion fuente = new FuenteAlimentacion();
			mapearBase(fuente, record);
			fuente.setTipo(texto(record, "type"));
			fuente.setEficiencia(texto(record, "efficiency"));
			fuente.setPotencia(parseInteger(record, "wattage"));
			fuente.setModular(texto(record, "modular"));
			fuente.setColor(texto(record, "color"));
			// Do not treat PSU as a consumer; consumoWatts stays null
			fuente.setConsumoWatts(null);
			entityManager.persist(fuente);
		}
	}

	private void importarRefrigeradoresCpu() throws IOException {
		for (CSVRecord record : leerCsv("data/cpu-cooler.csv")) {
			RefrigeradorCPU refrigerador = new RefrigeradorCPU();
			mapearBase(refrigerador, record);
			Double[] rpm = parseRango(texto(record, "rpm"));
			refrigerador.setRpmMin(rpm[0] == null ? null : rpm[0].intValue());
			refrigerador.setRpmMax(rpm[1] == null ? null : rpm[1].intValue());
			Double[] nivelRuido = parseRango(texto(record, "noise_level"));
			refrigerador.setNivelRuidoMin(nivelRuido[0]);
			refrigerador.setNivelRuidoMax(nivelRuido[1]);
			refrigerador.setColor(texto(record, "color"));
			refrigerador.setTamano(parseInteger(record, "size"));
			entityManager.persist(refrigerador);
		}
	}

	private void mapearBase(Componente componente, CSVRecord record) {
		componente.setNombre(texto(record, "name"));
		componente.setPrecio(parseDouble(record, "price"));
		// Map optional normalized consumption column if present (skip for PSU)
		if (!(componente instanceof FuenteAlimentacion) && record.isMapped("consumo")) {
			Integer consumo = parseInteger(record, "consumo");
			if (consumo != null) {
				componente.setConsumoWatts(consumo);
			}
		}
	}

	private List<CSVRecord> leerCsv(String classpathLocation) throws IOException {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
				CSVParser parser = CSVFormat.DEFAULT.builder()
						.setHeader()
						.setSkipHeaderRecord(true)
						.setTrim(true)
						.setIgnoreEmptyLines(true)
						.build()
						.parse(reader)) {
			return parser.getRecords();
		}
	}

	private String texto(CSVRecord record, String column) {
		if (!record.isMapped(column)) {
			return null;
		}

		String value = record.get(column);
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private Integer parseInteger(CSVRecord record, String column) {
		String value = texto(record, column);
		if (value == null) {
			return null;
		}

		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			try {
				Double d = Double.valueOf(value);
				return d.intValue();
			} catch (NumberFormatException ex) {
				return null;
			}
		}
	}

	private Double parseDouble(CSVRecord record, String column) {
		String value = texto(record, column);
		if (value == null) {
			return null;
		}

		return Double.valueOf(value);
	}

	/**
	 * Interpreta valores tipo rango del CSV: "600,3000" -> [600, 3000];
	 * un valor único "1550" -> [1550, 1550]; vacío o no numérico -> [null, null].
	 */
	private Double[] parseRango(String value) {
		if (value == null) {
			return new Double[] { null, null };
		}

		String[] partes = value.split(",");
		try {
			Double min = Double.valueOf(partes[0].trim());
			Double max = partes.length > 1 ? Double.valueOf(partes[1].trim()) : min;
			return new Double[] { min, max };
		} catch (NumberFormatException e) {
			return new Double[] { null, null };
		}
	}
}
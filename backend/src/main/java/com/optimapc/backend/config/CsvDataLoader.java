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
		gaming.setTipoUsoPrevisto("Gaming equilibrado");
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
		ofimatica.setTipoUsoPrevisto("Ofimática y estudio");
		ofimatica.setFavorita(Boolean.FALSE);
		agregarComponente(ofimatica, "CPU", procesador, 1);
		agregarComponente(ofimatica, "Placa base", placaBase, 1);
		agregarComponente(ofimatica, "RAM", memoriaRAM, 1);
		agregarComponente(ofimatica, "Almacenamiento", almacenamiento, 1);
		agregarComponente(ofimatica, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(ofimatica, "Caja", caja, 1);
		entityManager.persist(ofimatica);

		ConfiguracionPC edicion = new ConfiguracionPC();
		edicion.setTipoUsoPrevisto("Edición y creación de contenido");
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
		List<Premontado> existingPremontados = listar(Premontado.class);
		int objetivo = 28; // objetivo de premontados en la BD
		int existentes = existingPremontados == null ? 0 : existingPremontados.size();
		int aCrear = objetivo - existentes;
		if (aCrear <= 0) {
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

		// Crear algunos premontados base ya conocidos (si no existen)
		if (existentes == 0) {
			Premontado starter = new Premontado();
			starter.setTipoUsoPrevisto("Entrada para gaming ligero");
			starter.setFavorita(Boolean.TRUE);
			starter.setDescripcion("Equipo equilibrado para jugar en 1080p y uso diario.");
			starter.setMarca("OptimaPC");
			starter.setDescuento(10);
			starter.setSistemaOperativo(TipoSO.WINDOWS);
			starter.setStock(5);
			starter.setEsReacondicionado(Boolean.FALSE);
			starter.setUsosPrevistos(Set.of(TipoUso.GAMING, TipoUso.EDICION));
			agregarComponente(starter, "CPU", procesador, 1);
			agregarComponente(starter, "GPU", tarjetaGrafica, 1);
			agregarComponente(starter, "Placa base", placaBase, 1);
			agregarComponente(starter, "RAM", memoriaRAM, 2);
			agregarComponente(starter, "Almacenamiento", almacenamiento, 1);
			agregarComponente(starter, "Fuente", fuenteAlimentacion, 1);
			agregarComponente(starter, "Caja", caja, 1);
			agregarComponente(starter, "Refrigeración", refrigeradorCPU, 1);
			entityManager.persist(starter);
			existentes++;
			aCrear = objetivo - existentes;
		}

		asegurarPremontadoAhorroEsencial();
		asegurarPremontadoEquilibradoEconomico();
		asegurarPremontadoGamingEconomico();

		// Plantilla para generar premontados adicionales con variación
		TipoUso[] usosDisponibles = TipoUso.values();
		TipoSO[] sos = TipoSO.values();

		for (int i = 0; i < aCrear; i++) {
			Premontado p = new Premontado();
			int idx = existentes + i + 1;
			String nombre = "Premontado " + idx;
			p.setTipoUsoPrevisto(nombre + " - Configuración genérica");
			p.setFavorita(Boolean.FALSE);
			p.setDescripcion("Premontado de prueba número " + idx + ".");
			p.setMarca(idx % 3 == 0 ? "OptimaWork" : "OptimaPC");
			p.setDescuento((idx % 5) * 5);
			p.setSistemaOperativo(sos[idx % sos.length]);
			p.setStock(1 + (idx % 10));
			p.setEsReacondicionado((idx % 7) == 0);

			// seleccionar entre 1 y 3 usos previstos
			Set<TipoUso> usos = Set.of(usosDisponibles[idx % usosDisponibles.length]);
			if (idx % 2 == 0) {
				usos = Set.of(usosDisponibles[idx % usosDisponibles.length], usosDisponibles[(idx + 1) % usosDisponibles.length]);
			}
			if (idx % 5 == 0) {
				usos = Set.of(usosDisponibles[(idx) % usosDisponibles.length], usosDisponibles[(idx + 2) % usosDisponibles.length], usosDisponibles[(idx + 3) % usosDisponibles.length]);
			}
			p.setUsosPrevistos(usos);

			// agregar componentes básicos (cantidad variable)
			agregarComponente(p, "CPU", procesador, 1);
			if (idx % 3 != 0) {
				agregarComponente(p, "GPU", tarjetaGrafica, 1);
			}
			agregarComponente(p, "Placa base", placaBase, 1);
			agregarComponente(p, "RAM", memoriaRAM, 1 + (idx % 3));
			agregarComponente(p, "Almacenamiento", almacenamiento, 1 + ((idx + 1) % 2));
			agregarComponente(p, "Fuente", fuenteAlimentacion, 1);
			agregarComponente(p, "Caja", caja, 1);
			if (idx % 4 == 0) {
				agregarComponente(p, "Refrigeración", refrigeradorCPU, 1);
			}

			entityManager.persist(p);
		}
	}

	private void asegurarPremontadoAhorroEsencial() {
		if (existePremontadoConTipoUsoPrevisto("Ahorro esencial")) {
			return;
		}

		Procesador procesador = componenteMasBarato(Procesador.class,
				cpu -> "AM4".equals(cpu.getSocket()) && cpu.getGraficaIntegrada() != null && !cpu.getGraficaIntegrada().isBlank());
		PlacaBase placaBase = componenteMasBarato(PlacaBase.class,
				placa -> "AM4".equals(placa.getSocket()) && "DDR4".equalsIgnoreCase(placa.getTipoDDR()));
		MemoriaRAM memoriaRAM = componenteMasBarato(MemoriaRAM.class,
				ram -> "DDR4".equalsIgnoreCase(ram.getTipoDDR()) && ram.getGbPorModulo() != null && ram.getGbPorModulo() <= 8);
		Almacenamiento almacenamiento = componenteMasBarato(Almacenamiento.class,
				ssd -> "SSD".equalsIgnoreCase(ssd.getTipo()) && ssd.getCapacidad() != null && ssd.getCapacidad() <= 500);
		FuenteAlimentacion fuenteAlimentacion = componenteMasBarato(FuenteAlimentacion.class,
				fuente -> fuente.getPotencia() != null && fuente.getPotencia() >= 500);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("MICROATX"));

		Premontado premontado = new Premontado();
		premontado.setTipoUsoPrevisto("Ahorro esencial");
		premontado.setFavorita(Boolean.FALSE);
		premontado.setDescripcion("Equipo de entrada para ofimática, navegación y tareas básicas con un presupuesto contenido.");
		premontado.setMarca("OptimaPC");
		premontado.setDescuento(0);
		premontado.setSistemaOperativo(TipoSO.LINUX);
		premontado.setStock(12);
		premontado.setEsReacondicionado(Boolean.FALSE);
		premontado.setUsosPrevistos(Set.of(TipoUso.OFIMATICA, TipoUso.PROGRAMACION));
		agregarComponente(premontado, "CPU", procesador, 1);
		agregarComponente(premontado, "Placa base", placaBase, 1);
		agregarComponente(premontado, "RAM", memoriaRAM, 2);
		agregarComponente(premontado, "Almacenamiento", almacenamiento, 1);
		agregarComponente(premontado, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(premontado, "Caja", caja, 1);
		entityManager.persist(premontado);
	}

	private void asegurarPremontadoEquilibradoEconomico() {
		if (existePremontadoConTipoUsoPrevisto("Equilibrado económico")) {
			return;
		}

		Procesador procesador = componenteMasBarato(Procesador.class,
				cpu -> "AM4".equals(cpu.getSocket()) && cpu.getGraficaIntegrada() != null && !cpu.getGraficaIntegrada().isBlank());
		PlacaBase placaBase = componenteMasBarato(PlacaBase.class,
				placa -> "AM4".equals(placa.getSocket()) && "DDR4".equalsIgnoreCase(placa.getTipoDDR()));
		MemoriaRAM memoriaRAM = componenteMasBarato(MemoriaRAM.class,
				ram -> "DDR4".equalsIgnoreCase(ram.getTipoDDR()) && ram.getNumModulos() != null && ram.getNumModulos() == 2 && ram.getGbPorModulo() != null && ram.getGbPorModulo() == 16);
		Almacenamiento almacenamiento = componenteMasBarato(Almacenamiento.class,
				ssd -> "SSD".equalsIgnoreCase(ssd.getTipo()) && ssd.getCapacidad() != null && ssd.getCapacidad() >= 1000);
		FuenteAlimentacion fuenteAlimentacion = componenteMasBarato(FuenteAlimentacion.class,
				fuente -> fuente.getPotencia() != null && fuente.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("MICROATX"));

		Premontado premontado = new Premontado();
		premontado.setTipoUsoPrevisto("Equilibrado económico");
		premontado.setFavorita(Boolean.FALSE);
		premontado.setDescripcion("Configuración equilibrada para subir de nivel respecto a la gama de entrada sin disparar el presupuesto.");
		premontado.setMarca("OptimaPC");
		premontado.setDescuento(0);
		premontado.setSistemaOperativo(TipoSO.WINDOWS);
		premontado.setStock(10);
		premontado.setEsReacondicionado(Boolean.FALSE);
		premontado.setUsosPrevistos(Set.of(TipoUso.OFIMATICA, TipoUso.PROGRAMACION, TipoUso.EDICION));
		agregarComponente(premontado, "CPU", procesador, 1);
		agregarComponente(premontado, "Placa base", placaBase, 1);
		agregarComponente(premontado, "RAM", memoriaRAM, 2);
		agregarComponente(premontado, "Almacenamiento", almacenamiento, 1);
		agregarComponente(premontado, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(premontado, "Caja", caja, 1);
		entityManager.persist(premontado);
	}

	private void asegurarPremontadoGamingEconomico() {
		if (existePremontadoConTipoUsoPrevisto("Gaming económico")) {
			return;
		}

		Procesador procesador = componenteMasBarato(Procesador.class,
				cpu -> "AM4".equals(cpu.getSocket()) && cpu.getGraficaIntegrada() == null);
		TarjetaGrafica tarjetaGrafica = componenteMasBarato(TarjetaGrafica.class,
				gpu -> gpu.getPrecio() != null && gpu.getPrecio() <= 250.0);
		PlacaBase placaBase = componenteMasBarato(PlacaBase.class,
				placa -> "AM4".equals(placa.getSocket()) && "DDR4".equalsIgnoreCase(placa.getTipoDDR()));
		MemoriaRAM memoriaRAM = componenteMasBarato(MemoriaRAM.class,
				ram -> "DDR4".equalsIgnoreCase(ram.getTipoDDR()) && ram.getNumModulos() != null && ram.getNumModulos() == 2 && ram.getGbPorModulo() != null && ram.getGbPorModulo() == 16);
		Almacenamiento almacenamiento = componenteMasBarato(Almacenamiento.class,
				ssd -> "SSD".equalsIgnoreCase(ssd.getTipo()) && ssd.getCapacidad() != null && ssd.getCapacidad() >= 1000);
		FuenteAlimentacion fuenteAlimentacion = componenteMasBarato(FuenteAlimentacion.class,
				fuente -> fuente.getPotencia() != null && fuente.getPotencia() >= 600);
		Caja caja = componenteMasBarato(Caja.class,
				c -> c.getTipo() != null && c.getTipo().toUpperCase().contains("ATX"));

		Premontado premontado = new Premontado();
		premontado.setTipoUsoPrevisto("Gaming económico");
		premontado.setFavorita(Boolean.TRUE);
		premontado.setDescripcion("Equipo gaming económico por debajo de los mil euros para jugar en 1080p con una GPU de entrada.");
		premontado.setMarca("OptimaPC");
		premontado.setDescuento(5);
		premontado.setSistemaOperativo(TipoSO.WINDOWS);
		premontado.setStock(8);
		premontado.setEsReacondicionado(Boolean.FALSE);
		premontado.setUsosPrevistos(Set.of(TipoUso.GAMING, TipoUso.STREAMING));
		agregarComponente(premontado, "CPU", procesador, 1);
		agregarComponente(premontado, "GPU", tarjetaGrafica, 1);
		agregarComponente(premontado, "Placa base", placaBase, 1);
		agregarComponente(premontado, "RAM", memoriaRAM, 2);
		agregarComponente(premontado, "Almacenamiento", almacenamiento, 1);
		agregarComponente(premontado, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(premontado, "Caja", caja, 1);
		entityManager.persist(premontado);
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

	private boolean existePremontadoConTipoUsoPrevisto(String tipoUsoPrevisto) {
		Long total = entityManager.createQuery(
				"select count(p) from Premontado p where lower(p.tipoUsoPrevisto) = lower(:tipoUsoPrevisto)",
				Long.class)
				.setParameter("tipoUsoPrevisto", tipoUsoPrevisto)
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
			refrigerador.setRpm(texto(record, "rpm"));
			refrigerador.setNivelRuido(texto(record, "noise_level"));
			refrigerador.setColor(texto(record, "color"));
			refrigerador.setTamano(texto(record, "size"));
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
}
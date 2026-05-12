package com.optimapc.backend.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
		if (tieneFilas(Premontado.class)) {
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

		Premontado office = new Premontado();
		office.setTipoUsoPrevisto("Trabajo y estudio");
		office.setFavorita(Boolean.FALSE);
		office.setDescripcion("Premontado compacto y silencioso para productividad.");
		office.setMarca("OptimaPC");
		office.setDescuento(5);
		office.setSistemaOperativo(TipoSO.LINUX);
		office.setStock(8);
		office.setEsReacondicionado(Boolean.TRUE);
		office.setUsosPrevistos(Set.of(TipoUso.OFIMATICA, TipoUso.PROGRAMACION));
		agregarComponente(office, "CPU", procesador, 1);
		agregarComponente(office, "Placa base", placaBase, 1);
		agregarComponente(office, "RAM", memoriaRAM, 1);
		agregarComponente(office, "Almacenamiento", almacenamiento, 1);
		agregarComponente(office, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(office, "Caja", caja, 1);
		entityManager.persist(office);

		Premontado creator = new Premontado();
		creator.setTipoUsoPrevisto("Edición profesional");
		creator.setFavorita(Boolean.FALSE);
		creator.setDescripcion("Configuración pensada para edición de foto y vídeo.");
		creator.setMarca("OptimaPC");
		creator.setDescuento(15);
		creator.setSistemaOperativo(TipoSO.WINDOWS);
		creator.setStock(3);
		creator.setEsReacondicionado(Boolean.FALSE);
		creator.setUsosPrevistos(Set.of(TipoUso.EDICION, TipoUso.STREAMING, TipoUso.PROGRAMACION));
		agregarComponente(creator, "CPU", procesador, 1);
		agregarComponente(creator, "GPU", tarjetaGrafica, 1);
		agregarComponente(creator, "Placa base", placaBase, 1);
		agregarComponente(creator, "RAM", memoriaRAM, 2);
		agregarComponente(creator, "Almacenamiento", almacenamiento, 2);
		agregarComponente(creator, "Fuente", fuenteAlimentacion, 1);
		agregarComponente(creator, "Caja", caja, 1);
		agregarComponente(creator, "Refrigeración", refrigeradorCPU, 1);
		entityManager.persist(creator);
	}

	private void sembrarValoracionesSiNecesario() {
		if (tieneFilas(Valoracion.class)) {
			return;
		}

		List<Usuario> usuarios = listar(Usuario.class);
		List<Premontado> premontados = listar(Premontado.class);
		if (usuarios.size() < 5 || premontados.size() < 3) {
			return;
		}

		// Valoraciones para el primer premontado
		crearValoracion(usuarios.get(0), premontados.get(0), 5, 
			"Excelente ordenador, lleva funcionando perfectamente más de 6 meses. Muy recomendado para gaming.",
			LocalDateTime.of(2025, 4, 20, 14, 30));
		crearValoracion(usuarios.get(1), premontados.get(0), 4,
			"Muy bueno en general. El único inconveniente es el ruido del ventilador bajo carga, pero es tolerable.",
			LocalDateTime.of(2025, 4, 15, 10, 15));
		crearValoracion(usuarios.get(2), premontados.get(0), 5,
			"Perfectamente empaquetado y llegó sin problemas. El rendimiento es espectacular en los juegos más exigentes.",
			LocalDateTime.of(2025, 4, 10, 9, 0));

		// Valoraciones para el segundo premontado
		crearValoracion(usuarios.get(0), premontados.get(1), 4,
			"Muy buen PC para el precio. La refrigeración podría ser mejor pero funciona bien.",
			LocalDateTime.of(2025, 4, 5, 16, 45));
		crearValoracion(usuarios.get(3), premontados.get(1), 3,
			"Cumple lo que promete pero nada excepcional. Esperaba un poco más por este precio.",
			LocalDateTime.of(2025, 3, 28, 11, 20));

		// Valoraciones para el tercer premontado
		crearValoracion(usuarios.get(1), premontados.get(2), 5,
			"¡Increíble! La mejor compra que he hecho. Funciona perfectamente para edición de vídeo.",
			LocalDateTime.of(2025, 4, 8, 13, 10));
		crearValoracion(usuarios.get(2), premontados.get(2), 4,
			"Muy satisfecho con la compra. Buen rendimiento y servicio de atención muy rápido.",
			LocalDateTime.of(2025, 4, 1, 15, 30));
		crearValoracion(usuarios.get(4), premontados.get(2), 5,
			"Ordenador de gran calidad. Recomendado 100%. Embalaje y entrega impecables.",
			LocalDateTime.of(2025, 3, 25, 10, 0));
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

	private <T> List<T> listar(Class<T> entityClass) {
		return entityManager.createQuery("select e from " + entityClass.getSimpleName() + " e order by e.id", entityClass)
				.getResultList();
	}

	private <T> T primer(Class<T> entityClass) {
		return listar(entityClass).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("No hay datos semilla para " + entityClass.getSimpleName()));
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
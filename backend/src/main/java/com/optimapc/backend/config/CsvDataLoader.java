package com.optimapc.backend.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.optimapc.backend.modelo.Almacenamiento;
import com.optimapc.backend.modelo.Caja;
import com.optimapc.backend.modelo.Componente;
import com.optimapc.backend.modelo.FuenteAlimentacion;
import com.optimapc.backend.modelo.MemoriaRAM;
import com.optimapc.backend.modelo.PlacaBase;
import com.optimapc.backend.modelo.Procesador;
import com.optimapc.backend.modelo.RefrigeradorCPU;
import com.optimapc.backend.modelo.TarjetaGrafica;

@Component
public class CsvDataLoader implements ApplicationRunner {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		if (hayDatosSemilla()) {
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

	private boolean hayDatosSemilla() {
		return tieneFilas(Procesador.class)
				|| tieneFilas(TarjetaGrafica.class)
				|| tieneFilas(PlacaBase.class)
				|| tieneFilas(MemoriaRAM.class)
				|| tieneFilas(Almacenamiento.class)
				|| tieneFilas(Caja.class)
				|| tieneFilas(FuenteAlimentacion.class)
				|| tieneFilas(RefrigeradorCPU.class);
	}

	private boolean tieneFilas(Class<? extends Componente> entityClass) {
		Long total = entityManager.createQuery("select count(e) from " + entityClass.getSimpleName() + " e", Long.class)
				.getSingleResult();
		return total != null && total > 0;
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
			memoriaRAM.setVelocidad(texto(record, "speed").split(",")[0].trim());
			memoriaRAM.setTipoDDR("DDR" + texto(record, "speed").split(",")[1].trim());
			memoriaRAM.setModulos(texto(record, "modules"));
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
package com.optimapc.backend.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.optimapc.backend.catalogo.PremontadoRepository;
import com.optimapc.backend.chatbot.dto.ChatRequest;
import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.support.TestData;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String LLM_URL = "http://llm.test/chat";

    @Mock
    private PremontadoRepository premontadoRepository;

    private ChatService nuevoServicio(String apiKey) {
        return new ChatService(premontadoRepository, LLM_URL, "modelo-test", apiKey);
    }

    private MockRestServiceServer servidorDe(ChatService service) {
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        return MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void mensajeVacioDevuelveAvisoSinLlamarAlLlm() {
        ChatService service = nuevoServicio("");
        assertThat(service.generarRespuestaChat("   ", null))
                .contains("escribe una pregunta");
    }

    @Test
    void preguntaGeneralDevuelveLaRespuestaDelLlm() {
        ChatService service = nuevoServicio("");
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Te ayudo con eso\"}}]}",
                        MediaType.APPLICATION_JSON));

        String respuesta = service.generarRespuestaChat("buenos días", null);

        assertThat(respuesta).isEqualTo("Te ayudo con eso");
        server.verify();
    }

    @Test
    void preguntaDeCatalogoIncluyePremontadosYUsaApiKey() {
        ChatService service = nuevoServicio("clave-secreta");
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc())
                .thenReturn(List.of(TestData.premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING)));
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Recomiendo el Asus ROG\"}}]}",
                        MediaType.APPLICATION_JSON));

        List<ChatRequest.MensajeHistorial> historial = List.of(new ChatRequest.MensajeHistorial("user", "hola"));
        String respuesta = service.generarRespuestaChat("busco un pc gaming de 1000€", historial);

        assertThat(respuesta).isEqualTo("Recomiendo el Asus ROG");
        server.verify();
    }

    @Test
    void respuestaSinContenidoDevuelveFallback() {
        ChatService service = nuevoServicio("");
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThat(service.generarRespuestaChat("hola", null)).contains("no está disponible");
    }

    @Test
    void errorDelLlmDevuelveFallback() {
        ChatService service = nuevoServicio("");
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withServerError());

        assertThat(service.generarRespuestaChat("hola", null)).contains("no está disponible");
    }

    @Test
    void preguntaDeCatalogoSerializaPremontadosConDatosIncompletos() {
        ChatService service = nuevoServicio(null); // apiKey null -> rama del null en la cabecera Authorization

        // A: premontado "desnudo" sin componentes, sin usos, sin SO, marca null, sin descuento
        Premontado bare = new Premontado();
        bare.setId(1L);
        bare.setMarca(null);
        bare.setEsReacondicionado(false);

        // B: reacondicionado, con componentes de campos numéricos/texto nulos o en blanco
        Premontado parcial = new Premontado();
        parcial.setId(2L);
        parcial.setMarca("Marca");
        parcial.setEsReacondicionado(true);
        parcial.setSistemaOperativo(null);
        Procesador cpu = new Procesador();
        cpu.setId(10L);
        cpu.setNombre(""); // en blanco -> "No especificado"
        cpu.setPrecio(100.0); // nucleos/frecuencia/tdp null
        TarjetaGrafica gpu = new TarjetaGrafica();
        gpu.setId(11L);
        gpu.setNombre("GPU");
        gpu.setPrecio(100.0);
        MemoriaRAM ram = new MemoriaRAM();
        ram.setId(12L);
        ram.setNombre("RAM");
        ram.setPrecio(50.0);
        Almacenamiento sto = new Almacenamiento();
        sto.setId(13L);
        sto.setNombre("SSD");
        sto.setPrecio(50.0); // tipo/interfaz null
        addComponente(parcial, cpu);
        addComponente(parcial, gpu);
        addComponente(parcial, ram);
        addComponente(parcial, sto);

        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc()).thenReturn(List.of(bare, parcial));
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}",
                MediaType.APPLICATION_JSON));

        // "recomiend"/"barato" son keywords de catálogo; ninguno casa -> fallback a todos
        String respuesta = service.generarRespuestaChat("recomiendame algo barato", null);

        assertThat(respuesta).isEqualTo("ok");
        server.verify();
    }

    @Test
    void preguntaDeCatalogoFiltraPorMarca() {
        ChatService service = nuevoServicio("");
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc())
                .thenReturn(List.of(TestData.premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING)));
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", MediaType.APPLICATION_JSON));

        // sin precio ni uso, pero coincide por marca
        String respuesta = service.generarRespuestaChat("recomiendame algo de asus", null);

        assertThat(respuesta).isEqualTo("ok");
        server.verify();
    }

    @Test
    void preguntaDeCatalogoConPrecioLejanoCaeEnCoincidenciaDeUso() {
        ChatService service = nuevoServicio("");
        when(premontadoRepository.findAllByOrderByMarcaAscIdAsc())
                .thenReturn(List.of(TestData.premontadoCompleto(1L, "Asus", "ROG", 10, false, TipoUso.GAMING)));
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", MediaType.APPLICATION_JSON));

        // 5000€ queda fuera del ±40% del precio del premontado, pero "gaming" coincide por uso
        String respuesta = service.generarRespuestaChat("busco un pc gaming de 5000€", null);

        assertThat(respuesta).isEqualTo("ok");
        server.verify();
    }

    @Test
    void respuestaConContenidoEnBlancoDevuelveFallback() {
        ChatService service = nuevoServicio("");
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}", MediaType.APPLICATION_JSON));

        assertThat(service.generarRespuestaChat("hola", null)).contains("no está disponible");
    }

    @Test
    void respuestaSinMensajeDevuelveFallback() {
        ChatService service = nuevoServicio("");
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withSuccess(
                "{\"choices\":[{}]}", MediaType.APPLICATION_JSON));

        assertThat(service.generarRespuestaChat("hola", null)).contains("no está disponible");
    }

    @Test
    void respuestaSinChoicesDevuelveFallback() {
        ChatService service = nuevoServicio("");
        MockRestServiceServer server = servidorDe(service);
        server.expect(requestTo(LLM_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(service.generarRespuestaChat("hola", null)).contains("no está disponible");
    }

    private void addComponente(Premontado p, Componente c) {
        ConfiguracionComponente cc = new ConfiguracionComponente();
        cc.setComponente(c);
        cc.setCantidad(1);
        p.agregarComponente(cc);
    }
}

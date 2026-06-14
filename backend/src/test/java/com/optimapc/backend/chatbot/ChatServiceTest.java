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
}

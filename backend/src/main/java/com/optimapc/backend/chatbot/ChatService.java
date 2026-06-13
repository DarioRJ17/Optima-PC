package com.optimapc.backend.chatbot;

import com.optimapc.backend.chatbot.dto.ChatRequest;
import com.optimapc.backend.catalogo.PremontadoRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.optimapc.backend.domain.Almacenamiento;
import com.optimapc.backend.domain.Componente;
import com.optimapc.backend.domain.ConfiguracionComponente;
import com.optimapc.backend.domain.MemoriaRAM;
import com.optimapc.backend.domain.Premontado;
import com.optimapc.backend.domain.Procesador;
import com.optimapc.backend.domain.TarjetaGrafica;
import com.optimapc.backend.domain.TipoUso;
import com.optimapc.backend.domain.Valoracion;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String CHAT_FALLBACK = "Lo siento, el asistente no está disponible en este momento. "
            + "Por favor, consulta nuestro catálogo directamente.";
    private static final int CHAT_CONNECT_TIMEOUT_MS = 5000;
    private static final int CHAT_READ_TIMEOUT_MS = 60000;
    private static final int MAX_PREMONTADOS_CHAT = 8;
    private static final Pattern PRECIO_PATTERN = Pattern.compile(
            "(\\d{3,5})\\s*[€e]|[€e]\\s*(\\d{3,5})|(\\d{3,5})\\s+euro");

    private static final Map<TipoUso, List<String>> SINONIMOS_USO = Map.of(
            TipoUso.GAMING,       List.of("gaming", "jugar", "juegos", "gamer", "videojuegos"),
            TipoUso.EDICION,      List.of("editar", "edición", "edicion", "video", "vídeo", "diseño", "diseñar", "creativo"),
            TipoUso.OFIMATICA,    List.of("trabajo", "oficina", "ofimática", "ofimatica", "trabajar"),
            TipoUso.PROGRAMACION, List.of("programar", "programación", "programacion", "código", "codigo", "desarrollo"),
            TipoUso.STREAMING,    List.of("streaming", "stream", "twitch", "retransmitir", "emitir"));

    private static final List<String> KEYWORDS_CATALOGO = List.of(
            "€", "euro", "presupuesto", "precio", "barato", "económico", "caro",
            "recomiend", "diferencia", "comparar", "mejor", "vs",
            "cpu", "gpu", "ram", "procesador", "gráfica", "almacenamiento",
            "gaming", "jugar", "juegos", "editar", "video", "vídeo", "diseño",
            "trabajo", "oficina", "programar", "streaming", "twitch");

    private final PremontadoRepository premontadoRepository;
    private final RestTemplate restTemplate;
    private final String llmUrl;
    private final String llmModel;
    private final String llmApiKey;

    public ChatService(
            PremontadoRepository premontadoRepository,
            @Value("${llm.url:https://api.groq.com/openai/v1/chat/completions}") String llmUrl,
            @Value("${llm.model:llama-3.1-8b-instant}") String llmModel,
            @Value("${llm.api-key:}") String llmApiKey) {
        this.premontadoRepository = premontadoRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CHAT_CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(CHAT_READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(requestFactory);
        this.llmUrl = llmUrl;
        this.llmModel = llmModel;
        this.llmApiKey = llmApiKey;
    }

    @Transactional(readOnly = true)
    public String generarRespuestaChat(String mensajeUsuario, List<ChatRequest.MensajeHistorial> historial) {
        String mensaje = mensajeUsuario == null ? "" : mensajeUsuario.trim();
        if (mensaje.isEmpty()) {
            return "Por favor, escribe una pregunta para poder ayudarte.";
        }

        String lower = mensaje.toLowerCase(Locale.ROOT);

        String catalogo = "";
        if (esPreguntaDeCatalogo(lower)) {
            List<Premontado> premontados = premontadoRepository.findAllByOrderByMarcaAscIdAsc();
            inicializarColecciones(premontados);
            catalogo = serializarCatalogoParaChat(filtrarPremontadosRelevantes(premontados, lower));
        }

        List<Map<String, String>> mensajes = new ArrayList<>();
        mensajes.add(Map.of("role", "system", "content", construirSystemPrompt(catalogo)));

        if (historial != null) {
            for (ChatRequest.MensajeHistorial h : historial) {
                mensajes.add(Map.of("role", h.rol(), "content", h.contenido()));
            }
        }
        mensajes.add(Map.of("role", "user", "content", mensaje));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", llmModel);
        body.put("messages", mensajes);
        body.put("temperature", 0.3);
        body.put("max_tokens", 300);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (llmApiKey != null && !llmApiKey.isBlank()) {
            headers.set("Authorization", "Bearer " + llmApiKey);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GroqChatResponse> resp = restTemplate.postForEntity(
                    llmUrl, request, GroqChatResponse.class);

            GroqChatResponse chatResp = resp.getBody();
            if (chatResp == null || chatResp.choices() == null || chatResp.choices().isEmpty()
                    || chatResp.choices().get(0).message() == null
                    || chatResp.choices().get(0).message().content() == null
                    || chatResp.choices().get(0).message().content().isBlank()) {
                return CHAT_FALLBACK;
            }
            return chatResp.choices().get(0).message().content().trim();
        } catch (RestClientException ex) {
            log.error("Error al llamar al LLM [{}]: {}", llmUrl, ex.getMessage());
            return CHAT_FALLBACK;
        }
    }

    private boolean esPreguntaDeCatalogo(String lower) {
        return KEYWORDS_CATALOGO.stream().anyMatch(lower::contains);
    }

    private List<Premontado> filtrarPremontadosRelevantes(List<Premontado> todos, String lower) {
        Double precioRef = extraerPrecio(lower);

        List<Premontado> filtrados = todos.stream()
                .filter(p -> coincideConMensaje(p, lower, precioRef))
                .limit(MAX_PREMONTADOS_CHAT)
                .collect(Collectors.toList());

        return filtrados.isEmpty()
                ? todos.stream().limit(MAX_PREMONTADOS_CHAT).collect(Collectors.toList())
                : filtrados;
    }

    private boolean coincideConMensaje(Premontado p, String lower, Double precioRef) {
        if (precioRef != null) {
            double precio = getPrecioEfectivo(p);
            if (Math.abs(precio - precioRef) <= precioRef * 0.4) return true;
        }
        boolean usoCoincide = p.getUsosPrevistos().stream().anyMatch(uso -> {
            List<String> sinonimos = SINONIMOS_USO.getOrDefault(uso, List.of());
            return sinonimos.stream().anyMatch(lower::contains) || lower.contains(uso.name().toLowerCase());
        });
        if (usoCoincide) return true;
        return p.getMarca() != null && lower.contains(p.getMarca().toLowerCase());
    }

    private Double extraerPrecio(String texto) {
        Matcher m = PRECIO_PATTERN.matcher(texto);
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null) return Double.parseDouble(m.group(i));
            }
        }
        return null;
    }

    private double getPrecioEfectivo(Premontado premontado) {
        Double precioReducido = premontado.getPrecioReducido();
        return precioReducido != null ? precioReducido : premontado.getPrecio();
    }

    private String construirSystemPrompt(String catalogo) {
        String seccionCatalogo = catalogo == null || catalogo.isBlank()
                ? ""
                : "\nCATÁLOGO DISPONIBLE:\n" + catalogo;

        return """
                Eres el asistente de OptimaPc, una tienda de PCs premontados. Responde siempre en español y de forma concisa.

                Ayudas con dos cosas:
                1. Premontados del catálogo: comparaciones, recomendaciones por presupuesto y uso.
                2. Cómo usar la web de OptimaPc.

                Si la pregunta es sobre otro tema di exactamente: "Solo puedo ayudarte con los premontados o la web de OptimaPc."

                GUÍA DE LA WEB:

                FILTROS DEL CATÁLOGO:
                - Se encuentran en la página principal.
                - Permiten filtrar los premontados por precio, tipo de uso, marca y otras propiedades relevantes.

                VALORACIONES:
                - Accede a los detalles de un premontado para ver sus valoraciones.
                - Puedes ver las valoraciones sin estar registrado.
                - Las valoraciones verificadas son de usuarios que han comprado ese premontado.
                - Para publicar una valoración necesitas estar registrado.
                - Solo se puede hacer una valoración por usuario y producto.
                - Se puntúa del 1 al 5 y se puede añadir un comentario opcional.

                SISTEMA DE RECOMENDACIONES:
                - OptimaPc muestra recomendaciones personalizadas a los usuarios registrados.
                - Las recomendaciones se generan a partir de las acciones del usuario, por orden de importancia: compras, valoraciones, favoritos y formulario de registro.

                MONTAR TU PC:
                - Accede desde el menú de la barra de navegación superior.
                - Se muestran los tipos de componentes; al pulsar en cada tipo se listan los disponibles.
                - Los componentes se listan filtrando las incompatibilidades con los ya seleccionados.
                - Se muestran advertencias en componentes de riesgo, aunque se pueden seguir seleccionando.
                - A la derecha de la pantalla aparece un resumen de los componentes elegidos y un indicador de equilibrio de la configuración.

                RECUPERACIÓN DE CONTRASEÑA:
                - Disponible para usuarios registrados con un email válido.
                - Se envía un enlace al correo registrado para acceder a la página de cambio de contraseña.
                %s""".formatted(seccionCatalogo);
    }

    private String serializarCatalogoParaChat(List<Premontado> premontados) {
        return premontados.stream()
                .map(this::serializarPremontadoParaChat)
                .collect(Collectors.joining("\n"));
    }

    private String serializarPremontadoParaChat(Premontado premontado) {
        List<Valoracion> valoraciones = premontado.getValoraciones();
        double valoracionMedia = valoraciones.isEmpty()
                ? 0.0
                : valoraciones.stream().mapToInt(Valoracion::getPuntuacion).average().orElse(0.0);

        String usos = premontado.getUsosPrevistos().isEmpty()
                ? "No especificado"
                : premontado.getUsosPrevistos().stream().map(Enum::name).collect(Collectors.joining(", "));

        Procesador procesador = buscarComponente(premontado, Procesador.class);
        TarjetaGrafica grafica = buscarComponente(premontado, TarjetaGrafica.class);
        MemoriaRAM ram = buscarComponente(premontado, MemoriaRAM.class);
        Almacenamiento almacenamiento = buscarComponente(premontado, Almacenamiento.class);

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Premontado: " + valorSeguro(premontado.getMarca()));
        joiner.add("Precio: " + formatearDinero(premontado.getPrecio()) + "€ (con descuento: " + formatearDinero(premontado.getPrecioReducido()) + "€)");
        joiner.add("Usos: " + usos);
        joiner.add("SO: " + (premontado.getSistemaOperativo() != null ? premontado.getSistemaOperativo().name() : "No especificado"));
        joiner.add("Reacondicionado: " + (Boolean.TRUE.equals(premontado.getEsReacondicionado()) ? "sí" : "no"));
        joiner.add("Valoración: " + redondear(valoracionMedia) + "/5 (" + valoraciones.size() + " valoraciones)");
        joiner.add("CPU: " + describirProcesador(procesador));
        joiner.add("GPU: " + describirGrafica(grafica));
        joiner.add("RAM: " + describirRam(ram));
        joiner.add("Almacenamiento: " + describirAlmacenamiento(almacenamiento));
        joiner.add("---");
        return joiner.toString();
    }

    private <T extends Componente> T buscarComponente(Premontado premontado, Class<T> tipo) {
        return premontado.getComponentes().stream()
                .map(ConfiguracionComponente::getComponente)
                .filter(tipo::isInstance)
                .map(tipo::cast)
                .findFirst()
                .orElse(null);
    }

    private void inicializarColecciones(List<Premontado> premontados) {
        premontados.forEach(p -> {
            p.getValoraciones().size();
            p.getUsosPrevistos().size();
            p.getComponentes().forEach(cfg -> {
                if (cfg.getComponente() != null) {
                    cfg.getComponente().getNombre();
                    cfg.getComponente().getPrecio();
                }
            });
        });
    }

    private String describirProcesador(Procesador procesador) {
        if (procesador == null) return "No especificado";
        return "%s | %s núcleos | %sGHz | TDP %sW".formatted(
                valorSeguro(procesador.getNombre()),
                valorNumerico(procesador.getNucleos()),
                valorNumerico(procesador.getFrecuenciaBoost()),
                valorNumerico(procesador.getTdp()));
    }

    private String describirGrafica(TarjetaGrafica grafica) {
        if (grafica == null) return "No especificado";
        return "%s | %sGB | %sMHz".formatted(
                valorSeguro(grafica.getNombre()),
                valorNumerico(grafica.getMemoria()),
                valorNumerico(grafica.getFrecuenciaBoost()));
    }

    private String describirRam(MemoriaRAM ram) {
        if (ram == null) return "No especificado";
        return "%s | %sGB x%s | %sMHz | CAS %s".formatted(
                valorSeguro(ram.getNombre()),
                valorNumerico(ram.getGbPorModulo()),
                valorNumerico(ram.getNumModulos()),
                valorNumerico(ram.getVelocidad()),
                valorNumerico(ram.getLatenciaCAS()));
    }

    private String describirAlmacenamiento(Almacenamiento almacenamiento) {
        if (almacenamiento == null) return "No especificado";
        return "%s | %sGB | %s | %s".formatted(
                valorSeguro(almacenamiento.getNombre()),
                valorNumerico(almacenamiento.getCapacidad()),
                valorSeguro(almacenamiento.getTipo()),
                valorSeguro(almacenamiento.getInterfaz()));
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.isBlank() ? "No especificado" : valor;
    }

    private String valorNumerico(Number valor) {
        return valor == null ? "N/D" : valor.toString();
    }

    private String formatearDinero(Double valor) {
        if (valor == null) return "N/D";
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private double redondear(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record GroqChatMessage(String role, String content) {}
    private record GroqChatChoice(GroqChatMessage message) {}
    private record GroqChatResponse(List<GroqChatChoice> choices) {}
}

package py.sn.inmobiliaria.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente de la API REST de Gemini (método generateContent), con el HttpClient de Java.
 *
 * <ul>
 *   <li>La clave viaja en la cabecera {@code x-goog-api-key}, nunca en la URL: así no queda
 *       en logs de servidores intermedios ni en mensajes de error.</li>
 *   <li>Nunca se registra la clave. Ante un error se registra solo el código HTTP y el modelo.</li>
 *   <li>Ante un error temporal del servidor (HTTP 500, 502, 503 o 504, por ejemplo "alta demanda")
 *       se reintenta UNA vez tras una pausa breve. No se reintenta ante 429 (cuota agotada: reintentar
 *       solo la gastaría más) ni ante tiempo agotado (el visitante esperaría el doble).</li>
 *   <li>Se reduce al mínimo el "razonamiento" interno del modelo: para preguntas sobre propiedades
 *       no hace falta, y así cada consulta gasta menos cuota y la respuesta no se recorta.
 *       Gemini 2.5 usa {@code thinkingBudget = 0}; Gemini 3 usa {@code thinkingLevel}
 *       (por defecto "minimal", configurable con GEMINI_THINKING_LEVEL).</li>
 * </ul>
 */
public class GeminiClient implements ClienteIa {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    public static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final HttpClient http;
    private final String urlBase;
    private final String apiKey;
    private final String modelo;
    private final String nivelRazonamiento; // null: no se envía (solo modelos Gemini 3 en adelante)
    private final Duration timeout;

    /** Pausa antes del único reintento; las pruebas la acortan. */
    Duration pausaReintento = Duration.ofSeconds(2);

    public GeminiClient(String apiKey, String modelo, String nivelRazonamiento, Duration timeout) {
        this(URL_BASE, apiKey, modelo, nivelRazonamiento, timeout);
    }

    /** Permite apuntar a un servidor de prueba local, sin llamar a Google. */
    public GeminiClient(String urlBase, String apiKey, String modelo, String nivelRazonamiento, Duration timeout) {
        this.urlBase = urlBase;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.nivelRazonamiento = nivelRazonamiento;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public String generar(String instrucciones, String contenido) {
        try {
            return intentar(instrucciones, contenido);
        } catch (ErrorClienteIa e) {
            if (!e.reintentable()) {
                throw e;
            }
            log.info("Reintentando la consulta a Gemini (modelo {}) tras un error temporal", modelo);
            try {
                Thread.sleep(pausaReintento.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw e;
            }
            return intentar(instrucciones, contenido);
        }
    }

    private String intentar(String instrucciones, String contenido) {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/models/" + URLEncoder.encode(modelo, StandardCharsets.UTF_8)
                        + ":generateContent"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo(instrucciones, contenido), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.NO_DISPONIBLE, "Gemini no respondió a tiempo", e);
        } catch (IOException e) {
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.NO_DISPONIBLE, "No se pudo conectar con Gemini", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.NO_DISPONIBLE, "Consulta a Gemini interrumpida", e);
        }

        int estado = respuesta.statusCode();
        if (estado != 200) {
            log.warn("Gemini respondió HTTP {} (modelo {})", estado, modelo); // sin clave ni contenido
            if (estado == 429) {
                throw new ErrorClienteIa(ErrorClienteIa.Tipo.CUOTA_AGOTADA, "Cuota de Gemini agotada");
            }
            if (estado == 400 || estado == 401 || estado == 403 || estado == 404) {
                throw new ErrorClienteIa(ErrorClienteIa.Tipo.CONFIGURACION,
                        "Gemini rechazó la petición (HTTP " + estado + "): revise GEMINI_API_KEY y GEMINI_MODEL");
            }
            boolean temporal = estado == 500 || estado == 502 || estado == 503 || estado == 504;
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.NO_DISPONIBLE, "Error temporal de Gemini (HTTP " + estado + ")",
                    temporal);
        }
        return texto(respuesta.body());
    }

    /** Arma el JSON de generateContent. */
    String cuerpo(String instrucciones, String contenido) {
        ObjectNode raiz = JsonUtil.mapper().createObjectNode();
        raiz.putObject("system_instruction").putArray("parts").addObject().put("text", instrucciones);
        ArrayNode contents = raiz.putArray("contents");
        ObjectNode usuario = contents.addObject();
        usuario.put("role", "user");
        usuario.putArray("parts").addObject().put("text", contenido);

        ObjectNode generacion = raiz.putObject("generationConfig");
        generacion.put("temperature", 0.3);    // respuestas sobrias, apegadas a los datos
        generacion.put("maxOutputTokens", 600);
        if (modelo.startsWith("gemini-2.5")) {
            generacion.putObject("thinkingConfig").put("thinkingBudget", 0);
        } else if (nivelRazonamiento != null && !modelo.startsWith("gemini-1") && !modelo.startsWith("gemini-2")) {
            generacion.putObject("thinkingConfig").put("thinkingLevel", nivelRazonamiento);
        }
        try {
            return JsonUtil.mapper().writeValueAsString(raiz);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Extrae el texto de la respuesta (puede venir dividido en varias partes). */
    static String texto(String json) {
        JsonNode raiz;
        try {
            raiz = JsonUtil.mapper().readTree(json);
        } catch (IOException e) {
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.NO_DISPONIBLE, "Respuesta de Gemini con formato inesperado", e);
        }
        if (raiz.path("promptFeedback").hasNonNull("blockReason")) {
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.BLOQUEADA, "La consulta fue bloqueada por los filtros de Gemini");
        }
        JsonNode candidato = raiz.path("candidates").path(0);
        if ("SAFETY".equals(candidato.path("finishReason").asText())) {
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.BLOQUEADA, "La respuesta fue bloqueada por los filtros de Gemini");
        }
        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : candidato.path("content").path("parts")) {
            if (parte.hasNonNull("text") && !parte.path("thought").asBoolean(false)) {
                texto.append(parte.get("text").asText());
            }
        }
        if (texto.toString().isBlank()) {
            throw new ErrorClienteIa(ErrorClienteIa.Tipo.NO_DISPONIBLE, "Gemini devolvió una respuesta vacía");
        }
        return texto.toString().strip();
    }
}

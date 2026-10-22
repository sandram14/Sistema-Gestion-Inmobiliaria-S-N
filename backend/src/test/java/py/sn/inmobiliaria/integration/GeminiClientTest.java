package py.sn.inmobiliaria.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.util.JsonUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba el cliente real contra un servidor HTTP local que imita la API de Gemini:
 * se verifica la petición exacta que se enviaría a Google, sin gastar cuota.
 */
class GeminiClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> claveRecibida = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta = "";
    private volatile long demoraMs = 0;
    /** Si se define, las primeras peticiones reciben este estado (luego se usa "estado"). */
    private volatile int estadoInicial = 0;
    private volatile int peticionesConEstadoInicial = 0;
    private final java.util.concurrent.atomic.AtomicInteger peticiones = new java.util.concurrent.atomic.AtomicInteger();

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            rutaRecibida.set(intercambio.getRequestURI().toString());
            claveRecibida.set(intercambio.getRequestHeaders().getFirst("x-goog-api-key"));
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int numero = peticiones.incrementAndGet(); // se cuenta al recibirla, antes de cualquier demora
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int codigo = numero <= peticionesConEstadoInicial ? estadoInicial : estado;
            byte[] b = (codigo == 200 ? respuesta : "{\"error\":{\"code\":" + codigo + "}}").getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(codigo, b.length);
            intercambio.getResponseBody().write(b);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private GeminiClient cliente(String modelo, Duration timeout) {
        return cliente(modelo, "minimal", timeout);
    }

    private GeminiClient cliente(String modelo, String nivel, Duration timeout) {
        GeminiClient c = new GeminiClient("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta",
                "CLAVE-DE-PRUEBA", modelo, nivel, timeout);
        c.pausaReintento = Duration.ofMillis(10);
        return c;
    }

    private static String respuestaGemini(String texto) {
        return "{\"candidates\":[{\"content\":{\"role\":\"model\",\"parts\":[{\"text\":\"" + texto
                + "\"}]},\"finishReason\":\"STOP\"}]}";
    }

    @Test
    void enviaLaPeticionCorrectaYDevuelveElTexto() throws Exception {
        respuesta = respuestaGemini("La casa prop-001 cuesta 45000 USD.");

        String texto = cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("REGLAS", "DATOS Y PREGUNTA");

        assertEquals("La casa prop-001 cuesta 45000 USD.", texto);
        assertEquals("/v1beta/models/gemini-2.5-flash:generateContent", rutaRecibida.get());
        assertEquals("CLAVE-DE-PRUEBA", claveRecibida.get());           // la clave va en la cabecera...
        assertFalse(rutaRecibida.get().contains("CLAVE"));             // ...y nunca en la URL
        JsonNode cuerpo = JsonUtil.mapper().readTree(cuerpoRecibido.get());
        assertEquals("REGLAS", cuerpo.at("/system_instruction/parts/0/text").asText());
        assertEquals("DATOS Y PREGUNTA", cuerpo.at("/contents/0/parts/0/text").asText());
        assertEquals("user", cuerpo.at("/contents/0/role").asText());
        assertEquals(0, cuerpo.at("/generationConfig/thinkingConfig/thinkingBudget").asInt(-1)); // ahorra cuota
        assertTrue(cuerpo.at("/generationConfig/maxOutputTokens").asInt() > 0);
    }

    @Test
    void enGemini3EnviaElNivelDeRazonamientoConfigurado() throws Exception {
        respuesta = respuestaGemini("ok");

        cliente("gemini-3.1-flash-lite", Duration.ofSeconds(5)).generar("R", "C");

        JsonNode thinking = JsonUtil.mapper().readTree(cuerpoRecibido.get()).at("/generationConfig/thinkingConfig");
        assertEquals("minimal", thinking.path("thinkingLevel").asText());
        assertTrue(thinking.path("thinkingBudget").isMissingNode()); // el parámetro de 2.5 no se mezcla
        assertEquals("/v1beta/models/gemini-3.1-flash-lite:generateContent", rutaRecibida.get());
    }

    @Test
    void sinNivelConfiguradoNoEnviaThinkingConfig() throws Exception {
        respuesta = respuestaGemini("ok");

        cliente("gemini-3.5-flash", null, Duration.ofSeconds(5)).generar("R", "C");

        assertTrue(JsonUtil.mapper().readTree(cuerpoRecibido.get()).at("/generationConfig/thinkingConfig").isMissingNode());
    }

    @Test
    void unaRespuestaEnVariasPartesSeUneYSeIgnoraElRazonamiento() {
        respuesta = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"pienso...\",\"thought\":true},"
                + "{\"text\":\"Hola, \"},{\"text\":\"¿en qué ayudo?\"}]}}]}";

        assertEquals("Hola, ¿en qué ayudo?", cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("R", "C"));
    }

    @Test
    void cuotaAgotada() {
        estado = 429;
        respuesta = "{\"error\":{\"code\":429,\"status\":\"RESOURCE_EXHAUSTED\"}}";

        ErrorClienteIa e = assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("R", "C"));

        assertEquals(ErrorClienteIa.Tipo.CUOTA_AGOTADA, e.tipo());
    }

    @Test
    void claveOModeloInvalidosSonErrorDeConfiguracionSinMostrarLaClave() {
        estado = 400;
        respuesta = "{\"error\":{\"code\":400,\"message\":\"API key not valid\"}}";

        ErrorClienteIa e = assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("R", "C"));

        assertEquals(ErrorClienteIa.Tipo.CONFIGURACION, e.tipo());
        assertFalse(e.getMessage().contains("CLAVE-DE-PRUEBA"));
    }

    @Test
    void errorDelServidorYTiempoAgotadoSonNoDisponible() {
        estado = 503;
        respuesta = "{}";
        assertEquals(ErrorClienteIa.Tipo.NO_DISPONIBLE, assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("R", "C")).tipo());

        estado = 200;
        respuesta = respuestaGemini("tarde");
        demoraMs = 1500;
        int antes = peticiones.get();
        assertEquals(ErrorClienteIa.Tipo.NO_DISPONIBLE, assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-2.5-flash", Duration.ofMillis(300)).generar("R", "C")).tipo());
        assertEquals(antes + 1, peticiones.get()); // el tiempo agotado no se reintenta
    }

    @Test
    void unaAltaDemandaTemporalSeReintentaUnaVez() {
        estadoInicial = 503;
        peticionesConEstadoInicial = 1;   // la primera falla, la segunda responde
        respuesta = respuestaGemini("hola");

        assertEquals("hola", cliente("gemini-3.1-flash-lite", Duration.ofSeconds(5)).generar("R", "C"));
        assertEquals(2, peticiones.get());
    }

    @Test
    void siPersisteLaAltaDemandaSeRindeTrasUnSoloReintento() {
        estado = 503;

        ErrorClienteIa e = assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-3.1-flash-lite", Duration.ofSeconds(5)).generar("R", "C"));

        assertEquals(ErrorClienteIa.Tipo.NO_DISPONIBLE, e.tipo());
        assertEquals(2, peticiones.get());
    }

    @Test
    void cuotaAgotadaYErroresDeConfiguracionNoSeReintentan() {
        estado = 429;
        assertThrows(ErrorClienteIa.class, () -> cliente("gemini-3.1-flash-lite", Duration.ofSeconds(5)).generar("R", "C"));
        assertEquals(1, peticiones.get());

        estado = 404;
        assertThrows(ErrorClienteIa.class, () -> cliente("gemini-3.1-flash-lite", Duration.ofSeconds(5)).generar("R", "C"));
        assertEquals(2, peticiones.get());
    }

    @Test
    void contenidoBloqueadoYRespuestaVacia() {
        respuesta = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertEquals(ErrorClienteIa.Tipo.BLOQUEADA, assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("R", "C")).tipo());

        respuesta = "{\"candidates\":[{\"content\":{\"parts\":[]}}]}";
        assertEquals(ErrorClienteIa.Tipo.NO_DISPONIBLE, assertThrows(ErrorClienteIa.class,
                () -> cliente("gemini-2.5-flash", Duration.ofSeconds(5)).generar("R", "C")).tipo());
    }
}

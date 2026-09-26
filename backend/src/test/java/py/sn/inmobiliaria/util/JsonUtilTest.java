package py.sn.inmobiliaria.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.response.ErrorResponse;
import py.sn.inmobiliaria.dto.response.HealthResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilTest {

    /** DTO mínimo para probar la lectura de JSON de entrada. */
    record EjemploRequest(String titulo) {
    }

    @Test
    void fechasSeSerializanComoTextoIso() throws Exception {
        HealthResponse health = new HealthResponse("ok", "svc", "0.1.0", "desarrollo", "21",
                OffsetDateTime.of(2026, 9, 26, 10, 15, 30, 0, ZoneOffset.ofHours(-3)),
                HealthResponse.EstadoBaseDatos.noConfigurada());

        JsonNode json = JsonUtil.mapper().readTree(JsonUtil.mapper().writeValueAsString(health));

        assertTrue(json.get("horaServidor").isTextual());
        assertEquals("2026-09-26T10:15:30-03:00", json.get("horaServidor").asText());
    }

    @Test
    void estadoBaseDatosOmiteVersionesCuandoNoHay() throws Exception {
        JsonNode bd = JsonUtil.mapper().readTree(
                JsonUtil.mapper().writeValueAsString(HealthResponse.EstadoBaseDatos.error()));

        assertEquals("error", bd.get("estado").asText());
        assertFalse(bd.has("postgresql"));
        assertFalse(bd.has("postgis"));
    }

    @Test
    void errorSinCamposOmiteLaPropiedadCampos() throws Exception {
        String json = JsonUtil.mapper().writeValueAsString(ErrorResponse.de("NO_ENCONTRADO", "No existe"));

        JsonNode error = JsonUtil.mapper().readTree(json).get("error");
        assertEquals("NO_ENCONTRADO", error.get("codigo").asText());
        assertEquals("No existe", error.get("mensaje").asText());
        assertFalse(error.has("campos"));
    }

    @Test
    void errorConCamposLosIncluye() throws Exception {
        String json = JsonUtil.mapper().writeValueAsString(
                ErrorResponse.de("VALIDACION", "Datos inválidos", Map.of("precio", "debe ser mayor a 0")));

        JsonNode campos = JsonUtil.mapper().readTree(json).get("error").get("campos");
        assertEquals("debe ser mayor a 0", campos.get("precio").asText());
    }

    @Test
    void rechazaCamposDesconocidosEnLaEntrada() {
        String entrada = "{\"titulo\":\"Casa\",\"campoInventado\":1}";

        assertThrows(UnrecognizedPropertyException.class,
                () -> JsonUtil.mapper().readValue(entrada, EjemploRequest.class));
    }

    @Test
    void conservaTildesYEnies() throws Exception {
        String json = JsonUtil.mapper().writeValueAsString(new EjemploRequest("Casa en Área 1 - Ñemby"));

        assertEquals("Casa en Área 1 - Ñemby",
                JsonUtil.mapper().readValue(json, EjemploRequest.class).titulo());
    }
}

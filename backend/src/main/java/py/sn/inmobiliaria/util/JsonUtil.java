package py.sn.inmobiliaria.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * ObjectMapper de Jackson compartido por toda la aplicación.
 *
 * <p>Es seguro para uso concurrente una vez configurado, por eso existe una sola instancia.</p>
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            // Fechas como texto ISO-8601 ("2026-09-26T10:15:30-03:00"), no como números
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Rechaza campos desconocidos en los JSON de entrada: evita datos inesperados
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Números decimales sin notación científica: 45000 y no 4.5E+4
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}

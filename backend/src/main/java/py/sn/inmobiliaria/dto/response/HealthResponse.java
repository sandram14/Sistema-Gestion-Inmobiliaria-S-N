package py.sn.inmobiliaria.dto.response;

import java.time.OffsetDateTime;

/**
 * Respuesta de {@code GET /api/health}.
 *
 * @param estado       "ok" si la aplicación responde
 * @param servicio     nombre del backend
 * @param version      versión del artefacto (desde el pom)
 * @param entorno      valor de APP_ENV
 * @param java         versión de Java en ejecución
 * @param horaServidor fecha y hora del servidor
 */
public record HealthResponse(
        String estado,
        String servicio,
        String version,
        String entorno,
        String java,
        OffsetDateTime horaServidor) {
}

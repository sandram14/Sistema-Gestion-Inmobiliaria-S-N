package py.sn.inmobiliaria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

/**
 * Respuesta de {@code GET /api/health}.
 *
 * @param estado       "ok" si todo funciona; "degradado" si la base de datos no responde
 * @param servicio     nombre del backend
 * @param version      versión del artefacto (desde el pom)
 * @param entorno      valor de APP_ENV
 * @param java         versión de Java en ejecución
 * @param horaServidor fecha y hora del servidor
 * @param baseDatos    estado de la conexión con PostgreSQL/PostGIS
 */
public record HealthResponse(
        String estado,
        String servicio,
        String version,
        String entorno,
        String java,
        OffsetDateTime horaServidor,
        EstadoBaseDatos baseDatos) {

    /**
     * @param estado     ok | error | no_configurada
     * @param postgresql versión del servidor (solo si estado = ok)
     * @param postgis    versión de PostGIS (solo si estado = ok)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EstadoBaseDatos(String estado, String postgresql, String postgis) {

        public static final String OK = "ok";

        public static EstadoBaseDatos ok(String postgresql, String postgis) {
            return new EstadoBaseDatos(OK, postgresql, postgis);
        }

        public static EstadoBaseDatos error() {
            return new EstadoBaseDatos("error", null, null);
        }

        public static EstadoBaseDatos noConfigurada() {
            return new EstadoBaseDatos("no_configurada", null, null);
        }
    }
}

package py.sn.inmobiliaria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

/**
 * Respuesta de {@code GET /api/health}.
 *
 * @param estado       "ok" si todo funciona; "degradado" si la base o Hibernate fallan
 * @param servicio     nombre del backend
 * @param version      versión del artefacto (desde el pom)
 * @param entorno      valor de APP_ENV
 * @param java         versión de Java en ejecución
 * @param horaServidor fecha y hora del servidor
 * @param baseDatos    estado de la conexión con PostgreSQL/PostGIS
 * @param jpa          estado de Hibernate (esquema validado y lectura de entidades)
 */
public record HealthResponse(
        String estado,
        String servicio,
        String version,
        String entorno,
        String java,
        OffsetDateTime horaServidor,
        EstadoBaseDatos baseDatos,
        EstadoJpa jpa) {

    /**
     * @param estado                 ok | error | no_configurada
     * @param propiedadesDisponibles propiedades en estado Disponible leídas con Hibernate (solo si estado = ok)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EstadoJpa(String estado, Long propiedadesDisponibles) {

        public static EstadoJpa ok(long propiedadesDisponibles) {
            return new EstadoJpa(EstadoBaseDatos.OK, propiedadesDisponibles);
        }

        public static EstadoJpa error() {
            return new EstadoJpa("error", null);
        }

        public static EstadoJpa noConfigurada() {
            return new EstadoJpa("no_configurada", null);
        }
    }

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

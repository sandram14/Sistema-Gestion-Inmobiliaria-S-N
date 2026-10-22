package py.sn.inmobiliaria.service;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.ConflictoException;
import py.sn.inmobiliaria.exception.ReglaNegocioException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Map;

/**
 * Convierte errores técnicos de la base de datos en errores de la API con mensajes claros.
 *
 * <p>Por ejemplo, si un trigger rechaza "terreno en alquiler", el usuario recibe un 422 con
 * el mensaje del trigger, y si la base está caída, un 503; nunca un stack trace.</p>
 */
public final class TraductorErroresBd {

    /** Mensajes para restricciones UNIQUE (SQLState 23505), por nombre de restricción. */
    private static final Map<String, String> MENSAJES_UNICOS = Map.of(
            "propiedad_codigo_key", "Ya existe una propiedad con ese código",
            "uq_imagen_principal", "La propiedad ya tiene una imagen principal",
            "uq_imagen_url", "Esa imagen ya está registrada en la propiedad",
            "uq_usuario_email_lower", "Ya existe un usuario con ese email",
            "usuario_cedula_key", "Ya existe un usuario con esa cédula",
            "operacion_propiedad_pkey", "La propiedad ya tiene esa operación");

    private TraductorErroresBd() {
    }

    /**
     * @return la excepción de API equivalente, o la original si no es un error de base reconocible
     */
    public static RuntimeException traducir(RuntimeException error) {
        if (error instanceof ApiException) {
            return error;
        }
        SQLException sql = buscarSqlException(error);
        if (sql == null) {
            return error;
        }
        String estado = sql.getSQLState() == null ? "" : sql.getSQLState();

        if (sql instanceof SQLTransientConnectionException || estado.startsWith("08")) {
            return new ServicioNoDisponibleException(
                    "La base de datos no está disponible en este momento. Intente nuevamente.", error);
        }

        String restriccion = restriccion(sql);
        switch (estado) {
            case "23505": // unique_violation
                return new ConflictoException(MENSAJES_UNICOS.getOrDefault(restriccion,
                        "El dato ya existe y no puede repetirse"));
            case "23514": // check_violation (CHECK o trigger de regla de negocio)
                if (restriccion != null && restriccion.startsWith("regla_")) {
                    // Los triggers de /database/schema/04_triggers.sql tienen mensajes pensados para el usuario
                    return new ReglaNegocioException(restriccion.toUpperCase(), mensajeServidor(sql));
                }
                return new ReglaNegocioException("RESTRICCION", "Un valor no cumple las reglas de la base de datos");
            case "23503": // foreign_key_violation
                return new ConflictoException("El dato hace referencia a un registro que no existe o está en uso");
            case "23502": // not_null_violation
                return new ReglaNegocioException("DATO_OBLIGATORIO", "Falta un dato obligatorio");
            default:
                return error;
        }
    }

    private static SQLException buscarSqlException(Throwable t) {
        Throwable actual = t;
        while (actual != null) {
            if (actual instanceof SQLException s) {
                return s;
            }
            actual = actual.getCause();
        }
        return null;
    }

    private static String restriccion(SQLException sql) {
        if (sql instanceof PSQLException p) {
            ServerErrorMessage m = p.getServerErrorMessage();
            return m == null ? null : m.getConstraint();
        }
        return null;
    }

    private static String mensajeServidor(SQLException sql) {
        if (sql instanceof PSQLException p && p.getServerErrorMessage() != null
                && p.getServerErrorMessage().getMessage() != null) {
            return p.getServerErrorMessage().getMessage();
        }
        return "La operación no cumple una regla del negocio";
    }
}

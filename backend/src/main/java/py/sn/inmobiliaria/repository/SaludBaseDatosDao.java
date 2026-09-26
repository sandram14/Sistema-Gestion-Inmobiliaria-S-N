package py.sn.inmobiliaria.repository;

import java.sql.SQLException;

/**
 * Consulta el estado de la base de datos para el endpoint de salud.
 */
public interface SaludBaseDatosDao {

    /** Versiones del servidor; lanza SQLException si la base no responde. */
    InfoBaseDatos consultar() throws SQLException;

    /**
     * @param postgresql versión del servidor PostgreSQL
     * @param postgis    versión de la biblioteca PostGIS
     */
    record InfoBaseDatos(String postgresql, String postgis) {
    }
}

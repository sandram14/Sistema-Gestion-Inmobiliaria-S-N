package py.sn.inmobiliaria.repository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Consultas geográficas con PostGIS (SQL nativo vía JDBC).
 * Alimentan el mapa del frontend y la validación de ubicaciones.
 */
public interface UbicacionDao {

    /**
     * Ciudades con su punto central y cantidad de barrios, ordenadas por nombre.
     *
     * @param departamentoId departamento a filtrar; null devuelve todas
     */
    List<CiudadGeo> listarCiudades(Integer departamentoId) throws SQLException;

    /** Todas las ciudades. */
    default List<CiudadGeo> listarCiudades() throws SQLException {
        return listarCiudades(null);
    }

    /**
     * Barrios con su polígono en GeoJSON.
     *
     * @param ciudadId ciudad a filtrar; null devuelve todos
     */
    List<BarrioGeo> listarBarrios(Integer ciudadId) throws SQLException;

    /**
     * ¿El punto está dentro del barrio? (ST_Covers: incluye el borde).
     *
     * @return vacío si el barrio no existe
     */
    Optional<Boolean> puntoDentroDeBarrio(int idBarrio, BigDecimal latitud, BigDecimal longitud) throws SQLException;

    /**
     * Barrio que contiene el punto (ST_Covers: incluye el borde), con su ciudad y departamento.
     * Es la misma regla que aplica el trigger de la base al guardar una propiedad.
     * Si el punto cae justo en el límite entre dos barrios, devuelve el de menor id.
     *
     * @return vacío si el punto está fuera de todos los barrios registrados
     */
    Optional<FilaUbicacion> barrioQueContiene(BigDecimal latitud, BigDecimal longitud) throws SQLException;

    /** Jerarquía completa Departamento → Ciudad → Barrio, sin geometrías (para formularios). */
    List<FilaUbicacion> listarJerarquia() throws SQLException;

    /** Ciudad con su punto central (longitud/latitud en WGS84). */
    record CiudadGeo(int id, String nombre, String codigoDistrito, int idDepartamento, String departamento,
                     double longitud, double latitud, int totalBarrios) {
    }

    /** Barrio con su geometría ya convertida a texto GeoJSON por PostGIS. */
    record BarrioGeo(int id, String nombre, int idCiudad, String ciudad, String departamento,
                     String geometriaGeoJson) {
    }

    /** Una fila de la jerarquía (un barrio con su ciudad y departamento). */
    record FilaUbicacion(int idDepartamento, String departamento, int idCiudad, String ciudad,
                         int idBarrio, String barrio) {
    }
}

package py.sn.inmobiliaria.repository.impl;

import py.sn.inmobiliaria.repository.UbicacionDao;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Consultas PostGIS con JDBC y SQL parametrizado.
 *
 * <p>PostGIS convierte las geometrías a GeoJSON ({@code ST_AsGeoJSON}) con 6 decimales
 * (unos 10 cm de precisión), el mismo formato que ya usaba el mapa del frontend.</p>
 */
public class JdbcUbicacionDao implements UbicacionDao {

    private static final String SQL_CIUDADES = """
            SELECT c.id_ciudad, c.nombre, c.codigo_distrito, d.id_departamento, d.nombre AS departamento,
                   ST_X(c.centro) AS longitud, ST_Y(c.centro) AS latitud,
                   COUNT(b.id_barrio) AS total_barrios
              FROM ciudad c
              JOIN departamento d ON d.id_departamento = c.id_departamento
              LEFT JOIN barrio b  ON b.id_ciudad = c.id_ciudad
             WHERE (CAST(? AS integer) IS NULL OR d.id_departamento = CAST(? AS integer))
               AND (NOT CAST(? AS boolean) OR %s)
             GROUP BY c.id_ciudad, c.nombre, c.codigo_distrito, d.id_departamento, d.nombre, c.centro
             ORDER BY c.nombre
            """.formatted(JdbcUbicacionDao.CIUDAD_CON_PROPIEDADES);

    /** La ciudad {@code c} tiene al menos una propiedad Disponible (el sitio solo muestra esas ciudades). */
    private static final String CIUDAD_CON_PROPIEDADES = """
            EXISTS (SELECT 1
                      FROM propiedad p
                      JOIN estado_propiedad e ON e.id_estado = p.id_estado
                      JOIN barrio bp          ON bp.id_barrio = p.id_barrio
                     WHERE e.nombre = ? AND bp.id_ciudad = c.id_ciudad)""";

    /**
     * Tolerancia de simplificación para DIBUJAR los barrios (grados; 0.00003 ≈ 3 m): con los 714 barrios del
     * INE el GeoJSON pasa de unos 6 MB a 2,4 MB (0,3 MB las 4 ciudades). ST_SimplifyPreserveTopology evita
     * que un contorno se cruce consigo mismo al simplificarlo; ST_Multi lo devuelve siempre como MultiPolygon
     * (al simplificar, un barrio de una sola parte puede salir como Polygon). La validación de ubicaciones (trigger y
     * {@link #barrioQueContiene}) usa siempre el polígono exacto de la base.
     */
    static final double TOLERANCIA_DIBUJO = 0.00003;

    private static final String SQL_BARRIOS = """
            SELECT b.id_barrio, b.nombre, c.id_ciudad, c.nombre AS ciudad, d.nombre AS departamento,
                   ST_AsGeoJSON(ST_Multi(ST_SimplifyPreserveTopology(b.geom, ?)), 6) AS geojson
              FROM barrio b
              JOIN ciudad c       ON c.id_ciudad = b.id_ciudad
              JOIN departamento d ON d.id_departamento = c.id_departamento
             WHERE (CAST(? AS integer) IS NULL OR b.id_ciudad = CAST(? AS integer))
               AND (NOT CAST(? AS boolean) OR %s)
             ORDER BY c.nombre, b.nombre
            """.formatted(CIUDAD_CON_PROPIEDADES);

    /** Punto en WGS84 a partir de (longitud, latitud): el mismo que usa el trigger de la base. */
    private static final String PUNTO = "ST_SetSRID(ST_MakePoint(?, ?), 4326)";

    private static final String SQL_PUNTO_EN_BARRIO = """
            SELECT ST_Covers(geom, %s)
              FROM barrio
             WHERE id_barrio = ?
            """.formatted(PUNTO);

    /** Departamento, ciudad y barrio de cada barrio; compartido por la jerarquía y la detección por punto. */
    private static final String SELECT_FILA_UBICACION = """
            SELECT d.id_departamento, d.nombre, c.id_ciudad, c.nombre, b.id_barrio, b.nombre
              FROM departamento d
              JOIN ciudad c ON c.id_departamento = d.id_departamento
              JOIN barrio b ON b.id_ciudad = c.id_ciudad
            """;

    private static final String SQL_JERARQUIA = SELECT_FILA_UBICACION + " ORDER BY d.nombre, c.nombre, b.nombre";

    // ST_Covers usa el índice espacial ix_barrio_geom: no recorre todos los polígonos
    private static final String SQL_BARRIO_POR_PUNTO = SELECT_FILA_UBICACION
            + " WHERE ST_Covers(b.geom, " + PUNTO + ") ORDER BY b.id_barrio LIMIT 1";

    private final DataSource dataSource;

    public JdbcUbicacionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CiudadGeo> listarCiudades(Integer departamentoId, boolean soloConPropiedades) throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_CIUDADES)) {
            // El mismo parámetro dos veces: "sin filtro" (NULL) o "solo este departamento"
            ps.setObject(1, departamentoId, java.sql.Types.INTEGER);
            ps.setObject(2, departamentoId, java.sql.Types.INTEGER);
            ps.setBoolean(3, soloConPropiedades);
            ps.setString(4, py.sn.inmobiliaria.model.EstadoPropiedad.DISPONIBLE);
            try (ResultSet rs = ps.executeQuery()) {
                List<CiudadGeo> ciudades = new ArrayList<>();
                while (rs.next()) {
                    ciudades.add(new CiudadGeo(
                            rs.getInt("id_ciudad"),
                            rs.getString("nombre"),
                            rs.getString("codigo_distrito"),
                            rs.getInt("id_departamento"),
                            rs.getString("departamento"),
                            rs.getDouble("longitud"),
                            rs.getDouble("latitud"),
                            rs.getInt("total_barrios")));
                }
                return ciudades;
            }
        }
    }

    @Override
    public List<BarrioGeo> listarBarrios(Integer ciudadId, boolean soloConPropiedades) throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BARRIOS)) {
            ps.setDouble(1, TOLERANCIA_DIBUJO);
            // El mismo parámetro dos veces: "sin filtro" (NULL) o "solo esta ciudad"
            ps.setObject(2, ciudadId, java.sql.Types.INTEGER);
            ps.setObject(3, ciudadId, java.sql.Types.INTEGER);
            ps.setBoolean(4, soloConPropiedades);
            ps.setString(5, py.sn.inmobiliaria.model.EstadoPropiedad.DISPONIBLE);
            try (ResultSet rs = ps.executeQuery()) {
                List<BarrioGeo> barrios = new ArrayList<>();
                while (rs.next()) {
                    barrios.add(new BarrioGeo(
                            rs.getInt("id_barrio"),
                            rs.getString("nombre"),
                            rs.getInt("id_ciudad"),
                            rs.getString("ciudad"),
                            rs.getString("departamento"),
                            rs.getString("geojson")));
                }
                return barrios;
            }
        }
    }

    @Override
    public Optional<Boolean> puntoDentroDeBarrio(int idBarrio, BigDecimal latitud, BigDecimal longitud)
            throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_PUNTO_EN_BARRIO)) {
            ps.setDouble(1, longitud.doubleValue()); // ST_MakePoint(x = longitud, y = latitud)
            ps.setDouble(2, latitud.doubleValue());
            ps.setInt(3, idBarrio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBoolean(1)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<FilaUbicacion> barrioQueContiene(BigDecimal latitud, BigDecimal longitud) throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_BARRIO_POR_PUNTO)) {
            ps.setDouble(1, longitud.doubleValue()); // ST_MakePoint(x = longitud, y = latitud)
            ps.setDouble(2, latitud.doubleValue());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fila(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<FilaUbicacion> listarJerarquia() throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_JERARQUIA);
             ResultSet rs = ps.executeQuery()) {
            List<FilaUbicacion> filas = new ArrayList<>();
            while (rs.next()) {
                filas.add(fila(rs));
            }
            return filas;
        }
    }

    private static FilaUbicacion fila(ResultSet rs) throws SQLException {
        return new FilaUbicacion(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getInt(5), rs.getString(6));
    }
}

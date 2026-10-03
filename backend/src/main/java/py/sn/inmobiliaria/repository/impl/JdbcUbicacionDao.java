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
             GROUP BY c.id_ciudad, c.nombre, c.codigo_distrito, d.id_departamento, d.nombre, c.centro
             ORDER BY c.nombre
            """;

    private static final String SQL_BARRIOS = """
            SELECT b.id_barrio, b.nombre, c.id_ciudad, c.nombre AS ciudad, d.nombre AS departamento,
                   ST_AsGeoJSON(b.geom, 6) AS geojson
              FROM barrio b
              JOIN ciudad c       ON c.id_ciudad = b.id_ciudad
              JOIN departamento d ON d.id_departamento = c.id_departamento
            """;

    private static final String SQL_PUNTO_EN_BARRIO = """
            SELECT ST_Covers(geom, ST_SetSRID(ST_MakePoint(?, ?), 4326))
              FROM barrio
             WHERE id_barrio = ?
            """;

    private static final String SQL_JERARQUIA = """
            SELECT d.id_departamento, d.nombre, c.id_ciudad, c.nombre, b.id_barrio, b.nombre
              FROM departamento d
              JOIN ciudad c ON c.id_departamento = d.id_departamento
              JOIN barrio b ON b.id_ciudad = c.id_ciudad
             ORDER BY d.nombre, c.nombre, b.nombre
            """;

    private final DataSource dataSource;

    public JdbcUbicacionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CiudadGeo> listarCiudades(Integer departamentoId) throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_CIUDADES)) {
            // El mismo parámetro dos veces: "sin filtro" (NULL) o "solo este departamento"
            ps.setObject(1, departamentoId, java.sql.Types.INTEGER);
            ps.setObject(2, departamentoId, java.sql.Types.INTEGER);
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
    public List<BarrioGeo> listarBarrios(Integer ciudadId) throws SQLException {
        String sql = SQL_BARRIOS
                + (ciudadId != null ? " WHERE b.id_ciudad = ?" : "")
                + " ORDER BY c.nombre, b.nombre";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (ciudadId != null) {
                ps.setInt(1, ciudadId);
            }
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
    public List<FilaUbicacion> listarJerarquia() throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_JERARQUIA);
             ResultSet rs = ps.executeQuery()) {
            List<FilaUbicacion> filas = new ArrayList<>();
            while (rs.next()) {
                filas.add(new FilaUbicacion(
                        rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getInt(5), rs.getString(6)));
            }
            return filas;
        }
    }
}

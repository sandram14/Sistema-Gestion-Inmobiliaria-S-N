package py.sn.inmobiliaria.repository.impl;

import py.sn.inmobiliaria.repository.SaludBaseDatosDao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementación con JDBC directo: es una consulta técnica de una línea
 * y no necesita Hibernate ("JDBC cuando sea necesario").
 */
public class JdbcSaludBaseDatosDao implements SaludBaseDatosDao {

    private static final String SQL =
            "SELECT current_setting('server_version'), PostGIS_Lib_Version()";

    private final DataSource dataSource;

    public JdbcSaludBaseDatosDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public InfoBaseDatos consultar() throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setQueryTimeout(3); // segundos
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new InfoBaseDatos(rs.getString(1), rs.getString(2));
            }
        }
    }
}

package py.sn.inmobiliaria.service.sitio;

import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso JDBC al contenido editable del sitio (migración 006). */
public class ContenidoSitioDao {

    public record Destacada(long idPropiedad, String codigo, String titulo, String estado, int orden) {
    }

    private final DataSource ds;

    public ContenidoSitioDao(DataSource ds) {
        this.ds = ds;
    }

    public Optional<String> leer(String seccion) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT contenido FROM seccion_sitio WHERE seccion = ?")) {
            ps.setString(1, seccion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudo leer el contenido del sitio", e);
        }
    }

    public void guardar(String seccion, String json, Integer idUsuario) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE seccion_sitio SET contenido = ?, actualizado_en = now(), actualizado_por = ? WHERE seccion = ?")) {
            ps.setString(1, json);
            if (idUsuario == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, idUsuario);
            ps.setString(3, seccion);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("La sección " + seccion + " no existe (¿falta la migración 006?)");
            }
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudo guardar el contenido del sitio", e);
        }
    }

    /** Destacadas en orden, con su estado actual (para el panel). */
    public List<Destacada> destacadas() {
        String sql = "SELECT d.id_propiedad, p.codigo, p.titulo, e.nombre, d.orden FROM propiedad_destacada d "
                + "JOIN propiedad p USING (id_propiedad) JOIN estado_propiedad e ON e.id_estado = p.id_estado ORDER BY d.orden";
        List<Destacada> l = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                l.add(new Destacada(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)));
            }
            return l;
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudieron leer las propiedades destacadas", e);
        }
    }

    /** Ids (de la lista dada) que existen y están en estado Disponible. */
    public List<Long> disponibles(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT p.id_propiedad FROM propiedad p JOIN estado_propiedad e ON e.id_estado = p.id_estado "
                        + "WHERE e.nombre = 'Disponible' AND p.id_propiedad = ANY (?)")) {
            ps.setArray(1, c.createArrayOf("bigint", ids.toArray()));
            List<Long> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) l.add(rs.getLong(1));
            }
            return l;
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudieron verificar las propiedades", e);
        }
    }

    /** Reemplaza la selección completa (en una transacción). */
    public void reemplazarDestacadas(List<Long> ids) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement borrar = c.prepareStatement("DELETE FROM propiedad_destacada");
                 PreparedStatement insertar = c.prepareStatement("INSERT INTO propiedad_destacada (id_propiedad, orden) VALUES (?, ?)")) {
                borrar.executeUpdate();
                for (int i = 0; i < ids.size(); i++) {
                    insertar.setLong(1, ids.get(i));
                    insertar.setInt(2, i + 1);
                    insertar.addBatch();
                }
                insertar.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudo guardar la selección de destacadas", e);
        }
    }

    /** Códigos de las destacadas que siguen disponibles, en orden (para el sitio público). */
    public List<String> codigosDestacadosDisponibles() {
        String sql = "SELECT p.codigo FROM propiedad_destacada d JOIN propiedad p USING (id_propiedad) "
                + "JOIN estado_propiedad e ON e.id_estado = p.id_estado WHERE e.nombre = 'Disponible' ORDER BY d.orden";
        List<String> l = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) l.add(rs.getString(1));
            return l;
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudieron leer las propiedades destacadas", e);
        }
    }
}

package py.sn.inmobiliaria.prueba;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Barrio;
import py.sn.inmobiliaria.model.Casa;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.OperacionPropiedad;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Terreno;
import py.sn.inmobiliaria.model.TipoPropiedad;

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
 * Datos para las pruebas de integración que NO dependen de las propiedades de demostración.
 *
 * <p>Solo se apoya en los datos de referencia del sistema: catálogos (Casa/Terreno, estados,
 * operaciones) y la cartografía (departamentos, ciudades, barrios). Cada prueba crea sus
 * propias propiedades y compara contra el estado de la base antes de empezar, en lugar de
 * esperar cantidades fijas como "28 propiedades" o códigos como "prop-017".</p>
 */
public final class DatosPrueba {

    private DatosPrueba() {
    }

    /** Punto que no pertenece a ningún barrio de Paraguay (golfo de Guinea). */
    public static final BigDecimal LATITUD_FUERA = new BigDecimal("0.000000");
    public static final BigDecimal LONGITUD_FUERA = new BigDecimal("0.000000");

    /**
     * Un barrio registrado con un punto interior (ST_PointOnSurface), ya redondeado a la
     * precisión con la que se guardan las coordenadas.
     */
    public record Zona(int idBarrio, String barrio, int idCiudad, String ciudad, int idDepartamento,
                       String departamento, BigDecimal latitud, BigDecimal longitud) {
    }

    private static final String SQL_ZONAS = """
            SELECT b.id_barrio, b.nombre, c.id_ciudad, c.nombre, d.id_departamento, d.nombre,
                   round(ST_Y(ST_PointOnSurface(b.geom))::numeric, 6),
                   round(ST_X(ST_PointOnSurface(b.geom))::numeric, 6)
              FROM barrio b
              JOIN ciudad c       ON c.id_ciudad = b.id_ciudad
              JOIN departamento d ON d.id_departamento = c.id_departamento
             ORDER BY b.id_barrio
            """;

    /** Todos los barrios registrados, cada uno con un punto interior. */
    public static List<Zona> zonas(DataSource ds) {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_ZONAS);
             ResultSet rs = ps.executeQuery()) {
            List<Zona> zonas = new ArrayList<>();
            while (rs.next()) {
                zonas.add(new Zona(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getInt(5),
                        rs.getString(6), rs.getBigDecimal(7), rs.getBigDecimal(8)));
            }
            if (zonas.isEmpty()) {
                throw new IllegalStateException("La base no tiene barrios: revise database/seed/02_ubicacion.sql");
            }
            return zonas;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** El primer barrio registrado (por id). */
    public static Zona zona(DataSource ds) {
        return zonas(ds).get(0);
    }

    /** Un barrio de una ciudad distinta, si existe (para probar que los filtros excluyen). */
    public static Optional<Zona> zonaDeOtraCiudad(DataSource ds, int idCiudad) {
        return zonas(ds).stream().filter(z -> z.idCiudad() != idCiudad).findFirst();
    }

    /** Resultado numérico de una consulta SQL (por ejemplo, un count). */
    public static long numero(DataSource ds, String sql, Object... parametros) {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- Entidades de prueba (se guardan con em.persist dentro de la prueba) ----

    public static <T> T catalogo(EntityManager em, Class<T> clase, String nombre) {
        return em.createQuery("select x from " + clase.getSimpleName() + " x where x.nombre = :n", clase)
                .setParameter("n", nombre)
                .getSingleResult();
    }

    /** Casa Disponible de 3 habitaciones, 2 baños y 120 m², en el punto interior de la zona. */
    public static Propiedad casa(EntityManager em, String codigo, Zona z) {
        Propiedad p = base(em, codigo, TipoPropiedad.CASA, z);
        Casa casa = new Casa((short) 3, (short) 2, new BigDecimal("120"));
        casa.setCocina(true);
        p.asignarCasa(casa);
        return p;
    }

    /** Terreno Disponible de 500 m², en el punto interior de la zona. */
    public static Propiedad terreno(EntityManager em, String codigo, Zona z) {
        Propiedad p = base(em, codigo, TipoPropiedad.TERRENO, z);
        p.asignarTerreno(new Terreno(new BigDecimal("500")));
        return p;
    }

    public static OperacionPropiedad operacion(EntityManager em, String nombre, String precio, Moneda moneda) {
        return new OperacionPropiedad(catalogo(em, Operacion.class, nombre), new BigDecimal(precio), moneda);
    }

    private static Propiedad base(EntityManager em, String codigo, String tipo, Zona z) {
        return new Propiedad(codigo, "Propiedad de prueba " + codigo, z.latitud(), z.longitud(),
                catalogo(em, TipoPropiedad.class, tipo),
                catalogo(em, EstadoPropiedad.class, EstadoPropiedad.DISPONIBLE),
                em.find(Barrio.class, z.idBarrio()));
    }
}

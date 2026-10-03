package py.sn.inmobiliaria.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.prueba.DatosPrueba.Zona;
import py.sn.inmobiliaria.repository.UbicacionDao.BarrioGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.CiudadGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.FilaUbicacion;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración del DAO PostGIS (JDBC) contra la base real (solo si DB_URL está definida).
 *
 * <p>No usa cantidades ni nombres fijos: compara los resultados del DAO con lo que hay en la
 * base y verifica propiedades que deben cumplirse siempre (por ejemplo, "el punto interior de
 * cada barrio se detecta en ese mismo barrio").</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class UbicacionDaoIntegracionTest {

    private static HikariDataSource dataSource;
    private static UbicacionDao dao;

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        dao = new JdbcUbicacionDao(dataSource);
    }

    @AfterAll
    static void cerrar() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static long contar(String sql, Object... parametros) {
        return DatosPrueba.numero(dataSource, sql, parametros);
    }

    // ---- Ciudades y barrios ----

    @Test
    void listaTodasLasCiudadesConSuCentroYCantidadDeBarrios() throws Exception {
        List<CiudadGeo> ciudades = dao.listarCiudades();

        assertEquals(contar("SELECT count(*) FROM ciudad"), ciudades.size());
        for (CiudadGeo c : ciudades) {
            assertEquals(contar("SELECT count(*) FROM barrio WHERE id_ciudad = ?", c.id()), c.totalBarrios(), c.nombre());
            assertTrue(c.idDepartamento() > 0);
            assertTrue(Math.abs(c.latitud()) <= 90 && Math.abs(c.longitud()) <= 180, c.nombre());
        }
    }

    @Test
    void filtraCiudadesPorDepartamento() throws Exception {
        Zona z = DatosPrueba.zona(dataSource);

        List<CiudadGeo> delDepartamento = dao.listarCiudades(z.idDepartamento());

        assertEquals(contar("SELECT count(*) FROM ciudad WHERE id_departamento = ?", z.idDepartamento()),
                delDepartamento.size());
        assertTrue(delDepartamento.stream().allMatch(c -> c.idDepartamento() == z.idDepartamento()));
        assertEquals(0, dao.listarCiudades(-1).size());
        assertEquals(dao.listarCiudades().size(), dao.listarCiudades(null).size());
    }

    @Test
    void listaTodosLosBarriosConPoligonoGeoJson() throws Exception {
        List<BarrioGeo> barrios = dao.listarBarrios(null);

        assertEquals(contar("SELECT count(*) FROM barrio"), barrios.size());
        for (BarrioGeo b : barrios) {
            JsonNode geometria = JsonUtil.mapper().readTree(b.geometriaGeoJson());
            assertEquals("MultiPolygon", geometria.get("type").asText(), b.nombre());
            assertTrue(geometria.get("coordinates").size() > 0, b.nombre());
        }
    }

    @Test
    void filtraBarriosPorCiudad() throws Exception {
        Zona z = DatosPrueba.zona(dataSource);

        List<BarrioGeo> barrios = dao.listarBarrios(z.idCiudad());

        assertEquals(contar("SELECT count(*) FROM barrio WHERE id_ciudad = ?", z.idCiudad()), barrios.size());
        assertTrue(barrios.stream().allMatch(b -> b.idCiudad() == z.idCiudad()));
        assertTrue(barrios.stream().anyMatch(b -> b.id() == z.idBarrio()));
    }

    @Test
    void devuelveLaJerarquiaCompletaSinGeometrias() throws Exception {
        List<FilaUbicacion> filas = dao.listarJerarquia();

        assertEquals(contar("SELECT count(*) FROM barrio"), filas.size());
        assertTrue(filas.stream().allMatch(f -> f.idDepartamento() > 0 && f.idCiudad() > 0 && !f.barrio().isBlank()));
    }

    // ---- Punto dentro de un barrio dado (validación al guardar) ----

    @Test
    void validaSiUnPuntoEstaDentroDeUnBarrioDado() throws Exception {
        Zona z = DatosPrueba.zona(dataSource);

        assertEquals(Optional.of(true), dao.puntoDentroDeBarrio(z.idBarrio(), z.latitud(), z.longitud()));
        assertEquals(Optional.of(false), dao.puntoDentroDeBarrio(z.idBarrio(),
                DatosPrueba.LATITUD_FUERA, DatosPrueba.LONGITUD_FUERA));
        assertFalse(dao.puntoDentroDeBarrio(-1, z.latitud(), z.longitud()).isPresent());
    }

    // ---- Detección del barrio de un punto (Fase 13) ----

    @Test
    void detectaElBarrioDeUnPuntoInteriorDeCadaBarrio() throws Exception {
        for (Zona z : DatosPrueba.zonas(dataSource)) {
            FilaUbicacion f = dao.barrioQueContiene(z.latitud(), z.longitud()).orElseThrow(
                    () -> new AssertionError("No se detectó el barrio " + z.barrio()));

            assertEquals(z.idBarrio(), f.idBarrio(), z.barrio());
            assertEquals(z.idCiudad(), f.idCiudad(), z.barrio());
            assertEquals(z.idDepartamento(), f.idDepartamento(), z.barrio());
            assertEquals(z.ciudad(), f.ciudad());
            assertEquals(z.departamento(), f.departamento());
        }
    }

    @Test
    void unPuntoFueraDeTodosLosBarriosNoDevuelveNada() throws Exception {
        assertTrue(dao.barrioQueContiene(DatosPrueba.LATITUD_FUERA, DatosPrueba.LONGITUD_FUERA).isEmpty());
    }

    @Test
    void cadaPropiedadGuardadaSeDetectaEnSuPropioBarrio() throws Exception {
        // Coherencia con la validación al guardar: si el trigger aceptó la propiedad en su barrio,
        // la detección por punto debe devolver ese mismo barrio.
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT codigo, latitud, longitud, id_barrio FROM propiedad");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codigo = rs.getString(1);
                BigDecimal lat = rs.getBigDecimal(2);
                BigDecimal lng = rs.getBigDecimal(3);
                int barrio = rs.getInt(4);
                FilaUbicacion f = dao.barrioQueContiene(lat, lng).orElseThrow(
                        () -> new AssertionError(codigo + " no se detectó en ningún barrio"));
                assertTrue(dao.puntoDentroDeBarrio(f.idBarrio(), lat, lng).orElse(false), codigo);
                // Si el punto cae justo en el límite entre dos barrios, ambos lo "cubren";
                // en ese caso basta con que el barrio guardado también lo contenga.
                if (f.idBarrio() != barrio) {
                    assertTrue(dao.puntoDentroDeBarrio(barrio, lat, lng).orElse(false), codigo);
                }
            }
        }
    }
}

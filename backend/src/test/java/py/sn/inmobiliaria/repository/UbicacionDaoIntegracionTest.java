package py.sn.inmobiliaria.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.repository.UbicacionDao.BarrioGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.CiudadGeo;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.util.JsonUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración del DAO PostGIS (JDBC) contra la base real.
 * Solo se ejecuta si está definida la variable DB_URL (ver README del backend).
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

    private static CiudadGeo ciudad(String nombre) throws Exception {
        return dao.listarCiudades().stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    @Test
    void listaLasCuatroCiudadesConSuCentroYBarrios() throws Exception {
        List<CiudadGeo> ciudades = dao.listarCiudades();

        assertEquals(4, ciudades.size());
        CiudadGeo cde = ciudad("CIUDAD DEL ESTE");
        assertEquals("01", cde.codigoDistrito());
        assertEquals("ALTO PARANÁ", cde.departamento());
        assertEquals(4, cde.totalBarrios());
        assertEquals(-54.641961, cde.longitud(), 0.000001);
        assertEquals(-25.505118, cde.latitud(), 0.000001);
    }

    @Test
    void listaTodosLosBarriosConPoligonoGeoJson() throws Exception {
        List<BarrioGeo> barrios = dao.listarBarrios(null);

        assertEquals(12, barrios.size());
        JsonNode geometria = JsonUtil.mapper().readTree(barrios.get(0).geometriaGeoJson());
        assertEquals("MultiPolygon", geometria.get("type").asText());
        assertTrue(geometria.get("coordinates").size() > 0);
    }

    @Test
    void filtraBarriosPorCiudad() throws Exception {
        CiudadGeo cde = ciudad("CIUDAD DEL ESTE");

        List<BarrioGeo> barrios = dao.listarBarrios(cde.id());

        assertEquals(4, barrios.size());
        assertTrue(barrios.stream().allMatch(b -> b.idCiudad() == cde.id()));
        assertTrue(barrios.stream().anyMatch(b -> b.id() == 10011013 && b.nombre().equals("AREA 1")));
    }

    @Test
    void validaSiUnPuntoEstaDentroDelBarrio() throws Exception {
        // prop-001 está en AREA 1 (10011013): sus coordenadas deben estar dentro del polígono
        BigDecimal lat = obtenerLat("prop-001");
        BigDecimal lng = obtenerLng("prop-001");

        Optional<Boolean> dentro = dao.puntoDentroDeBarrio(10011013, lat, lng);
        Optional<Boolean> fuera = dao.puntoDentroDeBarrio(10011013, new BigDecimal("-25.300000"), new BigDecimal("-54.500000"));
        Optional<Boolean> inexistente = dao.puntoDentroDeBarrio(99999999, lat, lng);

        assertEquals(Optional.of(true), dentro);
        assertEquals(Optional.of(false), fuera);
        assertFalse(inexistente.isPresent());
    }

    @Test
    void devuelveLaJerarquiaCompletaSinGeometrias() throws Exception {
        List<UbicacionDao.FilaUbicacion> filas = dao.listarJerarquia();

        assertEquals(12, filas.size());
        assertTrue(filas.stream().allMatch(f -> f.departamento().equals("ALTO PARANÁ")));
    }

    // Coordenadas reales de una propiedad, leídas directamente con JDBC
    private static BigDecimal obtenerLat(String codigo) throws Exception {
        return coordenada(codigo, "latitud");
    }

    private static BigDecimal obtenerLng(String codigo) throws Exception {
        return coordenada(codigo, "longitud");
    }

    private static BigDecimal coordenada(String codigo, String columna) throws Exception {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement("SELECT " + columna + " FROM propiedad WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}

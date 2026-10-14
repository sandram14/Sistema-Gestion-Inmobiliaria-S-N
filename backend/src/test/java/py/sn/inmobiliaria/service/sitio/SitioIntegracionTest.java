package py.sn.inmobiliaria.service.sitio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.prueba.DatosPrueba;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Contenido del sitio y destacadas contra la base real (requiere la migración 006). Restaura todo al terminar. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class SitioIntegracionTest {

    private static HikariDataSource ds;
    private static ContenidoSitioDao dao;
    private static ContenidoSitioService servicio;
    private String inicioOriginal;
    private List<Long> destacadasOriginales;
    private final String marca = UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void iniciar() {
        ds = DataSourceFactory.crear(new BaseDatosConfig(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        dao = new ContenidoSitioDao(ds);
        servicio = new ContenidoSitioService(dao);
    }

    @AfterAll
    static void cerrar() {
        if (ds != null) ds.close();
    }

    @BeforeEach
    void guardarEstado() {
        inicioOriginal = dao.leer("INICIO").orElseThrow();
        destacadasOriginales = dao.destacadas().stream().map(ContenidoSitioDao.Destacada::idPropiedad).toList();
    }

    @AfterEach
    void restaurar() {
        dao.guardar("INICIO", inicioOriginal, null);
        dao.reemplazarDestacadas(destacadasOriginales);
        DatosPrueba.numero(ds, "WITH x AS (DELETE FROM propiedad WHERE titulo LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca);
    }

    @Test
    void lasSeccionesTienenElContenidoInicialYSeGuardanValidadas() throws Exception {
        assertFalse(servicio.leer("inicio").path("titulo").asText().isBlank());
        assertEquals(4, servicio.leer("servicios").path("servicios").size());
        JsonNode nuevo = new ObjectMapper().readTree("{\"imagenPortada\":null,\"titulo\":\"Título " + marca
                + "\",\"subtitulo\":\"\"}");
        int admin = (int) DatosPrueba.numero(ds, "SELECT min(id_usuario) FROM usuario");
        servicio.guardar("inicio", nuevo, admin);
        assertEquals(1, DatosPrueba.numero(ds, "SELECT count(*) FROM seccion_sitio WHERE seccion = 'INICIO' AND actualizado_por = ?", admin));
        assertEquals("Título " + marca, servicio.publico().path("inicio").path("titulo").asText());
    }

    @Test
    void soloSeDestacanPropiedadesDisponiblesYLasQueDejanDeEstarloSalenDeLaPortada() {
        long disponible = DatosPrueba.numero(ds, "INSERT INTO propiedad (codigo, titulo, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio) "
                + "SELECT 'test-' || (8000 + floor(random() * 999))::int, ?, latitud, longitud, id_tipo_propiedad, "
                + "(SELECT id_estado FROM estado_propiedad WHERE nombre = 'Disponible'), id_barrio FROM propiedad ORDER BY id_propiedad LIMIT 1 RETURNING id_propiedad",
                "Disponible " + marca);
        long vendida = DatosPrueba.numero(ds, "INSERT INTO propiedad (codigo, titulo, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio) "
                + "SELECT 'test-' || (9000 + floor(random() * 999))::int, ?, latitud, longitud, id_tipo_propiedad, "
                + "(SELECT id_estado FROM estado_propiedad WHERE nombre = 'Vendido'), id_barrio FROM propiedad ORDER BY id_propiedad LIMIT 1 RETURNING id_propiedad",
                "Vendida " + marca);

        assertThrows(ValidacionException.class, () -> servicio.guardarDestacadas(List.of(disponible, vendida)));
        assertEquals(1, servicio.guardarDestacadas(List.of(disponible)).size());
        String codigo = servicio.destacadas().get(0).codigo();
        assertTrue(servicio.publico().path("ofertas").toString().contains(codigo));

        DatosPrueba.numero(ds, "WITH x AS (UPDATE propiedad SET id_estado = (SELECT id_estado FROM estado_propiedad WHERE nombre = 'Vendido') "
                + "WHERE id_propiedad = ? RETURNING 1) SELECT count(*) FROM x", disponible);
        assertFalse(servicio.publico().path("ofertas").toString().contains(codigo)); // se vendió: sale sola de la portada
    }
}

package py.sn.inmobiliaria.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.dto.response.CatalogosDto;
import py.sn.inmobiliaria.dto.response.admin.PropiedadDetalleDto;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.service.CatalogoService;
import py.sn.inmobiliaria.service.PropiedadAdminService;
import py.sn.inmobiliaria.service.PropiedadConsultaService;
import py.sn.inmobiliaria.service.UbicacionService;
import py.sn.inmobiliaria.util.JsonUtil;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La prueba más importante de la integración: compara el GeoJSON que generará la API
 * con el archivo original del frontend (frontend/public/data/propiedades.geojson),
 * propiedad por propiedad y campo por campo.
 *
 * <p>Única diferencia esperada (decisión de la Fase 2): las 7 propiedades de categoría
 * "Departamento" ahora son "Casa".</p>
 *
 * <p>Solo se ejecuta si DB_URL está definida. Maven ejecuta las pruebas desde la carpeta
 * backend/, por eso el archivo original se busca en ../frontend.</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class CompatibilidadFrontendIntegracionTest {

    private static final File ORIGINAL = new File("../frontend/public/data/propiedades.geojson");

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static PropiedadConsultaService consulta;
    private static PropiedadAdminService admin;
    private static CatalogoService catalogos;

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        TxRunner tx = new TxRunner(jpa);
        JdbcUbicacionDao ubicacionDao = new JdbcUbicacionDao(dataSource);
        consulta = new PropiedadConsultaService(tx, new JpaPropiedadDao());
        admin = new PropiedadAdminService(tx, new JpaPropiedadDao(), new JpaCatalogoDao(), new JpaImagenDao(), ubicacionDao);
        catalogos = new CatalogoService(tx, new JpaCatalogoDao(), new UbicacionService(ubicacionDao));
    }

    @AfterAll
    static void cerrar() {
        if (jpa != null) {
            jpa.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static JsonNode json(Object o) throws Exception {
        return JsonUtil.mapper().readTree(JsonUtil.mapper().writeValueAsString(o));
    }

    @Test
    void lasPropiedadesDeLaApiCoincidenConElGeoJsonOriginalDelFrontend() throws Exception {
        assertTrue(ORIGINAL.exists(), "No se encontró " + ORIGINAL.getAbsolutePath());
        Map<String, JsonNode> originales = new HashMap<>();
        JsonUtil.mapper().readTree(ORIGINAL).get("features")
                .forEach(f -> originales.put(f.get("properties").get("id").asText(), f));

        JsonNode api = json(py.sn.inmobiliaria.dto.response.geojson.FeatureCollection.de(
                consulta.buscar(BusquedaRequest.vacia(), GeoJsonMapper.propiedad(null))));

        assertEquals(originales.size(), api.get("features").size());
        int departamentosConvertidos = 0;
        for (JsonNode f : api.get("features")) {
            JsonNode p = f.get("properties");
            String id = p.get("id").asText();
            JsonNode o = originales.get(id).get("properties");

            for (String campo : List.of("titulo", "tipoOperacion", "ciudad", "barrio", "moneda", "direccion")) {
                assertEquals(o.get(campo).asText(), p.get(campo).asText(), id + "." + campo);
            }
            for (String campo : List.of("barrioId", "dormitorios", "banos")) {
                assertEquals(o.get(campo).asInt(), p.get(campo).asInt(), id + "." + campo);
            }
            for (String campo : List.of("precio", "m2")) {
                assertEquals(0, new BigDecimal(o.get(campo).asText()).compareTo(new BigDecimal(p.get(campo).asText())),
                        id + "." + campo);
            }
            assertEquals(o.get("fotos"), p.get("fotos"), id + ".fotos");
            assertEquals(originales.get(id).get("geometry").get("coordinates").get(0).asDouble(),
                    f.get("geometry").get("coordinates").get(0).asDouble(), 1e-6, id + ".longitud");
            assertEquals(originales.get(id).get("geometry").get("coordinates").get(1).asDouble(),
                    f.get("geometry").get("coordinates").get(1).asDouble(), 1e-6, id + ".latitud");

            String categoriaOriginal = o.get("categoria").asText();
            if ("Departamento".equals(categoriaOriginal)) {
                assertEquals("Casa", p.get("categoria").asText(), id + ".categoria");
                departamentosConvertidos++;
            } else {
                assertEquals(categoriaOriginal, p.get("categoria").asText(), id + ".categoria");
            }
        }
        assertEquals(7, departamentosConvertidos);
    }

    @Test
    void elDetalleAdministrativoTraeTodoLoNecesarioParaEditar() {
        long id = consulta.obtenerPorCodigo("prop-001", p -> p.getId());

        PropiedadDetalleDto d = admin.obtener(id, PropiedadAdminMapper::detalle);

        assertEquals("prop-001", d.codigo());
        assertEquals("CIUDAD DEL ESTE", d.ubicacion().ciudad());
        assertEquals(5, d.imagenes().size());
        assertTrue(d.imagenes().get(0).esPrincipal());
        assertEquals(1, d.operaciones().size());
    }

    @Test
    void losCatalogosSeEntreganComoArbol() {
        CatalogosDto c = CatalogoMapper.dto(catalogos.obtener());

        assertEquals(1, c.departamentos().size());
        assertEquals(4, c.departamentos().get(0).ciudades().size());
        assertEquals(12, c.departamentos().get(0).ciudades().stream().mapToInt(ci -> ci.barrios().size()).sum());
    }
}

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
import py.sn.inmobiliaria.dto.response.admin.PropiedadResumenDto;
import py.sn.inmobiliaria.dto.response.geojson.FeatureCollection;
import py.sn.inmobiliaria.prueba.DatosPrueba;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compatibilidad del GeoJSON de la API con lo que espera el frontend.
 *
 * <ul>
 *   <li><b>Siempre</b> (con DB_URL): cada propiedad que devuelve la API tiene los campos y tipos
 *       de {@code PropiedadProps} (types.ts), sin importar qué propiedades haya en la base.</li>
 *   <li><b>Opcional</b> (además VERIFICAR_DATOS_DEMO=true): compara campo por campo con el
 *       archivo original frontend/public/data/propiedades.geojson. Sirve para confirmar la
 *       migración de los datos de demostración; deja de tener sentido cuando se editan desde
 *       el panel, por eso no se ejecuta por defecto.</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class CompatibilidadFrontendIntegracionTest {

    private static final File ORIGINAL = new File("../frontend/public/data/propiedades.geojson");

    /** Campos de PropiedadProps en frontend/src/components/PropertyMap/types.ts. */
    private static final Set<String> CAMPOS_FRONTEND = Set.of("id", "titulo", "categoria", "tipoOperacion", "ciudad",
            "barrio", "barrioId", "precio", "moneda", "dormitorios", "banos", "m2", "direccion", "fotos");

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

    private static JsonNode propiedadesDeLaApi() throws Exception {
        return JsonUtil.mapper().readTree(JsonUtil.mapper().writeValueAsString(FeatureCollection.de(
                consulta.buscar(BusquedaRequest.vacia(), GeoJsonMapper.propiedad(null)))));
    }

    private static Set<String> campos(JsonNode n) {
        Set<String> s = new HashSet<>();
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
            s.add(it.next());
        }
        return s;
    }

    @Test
    void cadaPropiedadTieneLaFormaQueEsperaElFrontend() throws Exception {
        JsonNode api = propiedadesDeLaApi();

        assertEquals("FeatureCollection", api.get("type").asText());
        for (JsonNode f : api.get("features")) {
            JsonNode p = f.get("properties");
            String id = p.get("id").asText();
            assertTrue(campos(p).containsAll(CAMPOS_FRONTEND), id + ": faltan campos de PropiedadProps");

            assertEquals("Point", f.get("geometry").get("type").asText(), id);
            assertEquals(2, f.get("geometry").get("coordinates").size(), id);
            assertTrue(p.get("id").isTextual() && p.get("titulo").isTextual() && p.get("direccion").isTextual(), id);
            assertTrue(Set.of("Casa", "Terreno").contains(p.get("categoria").asText()), id);
            assertTrue(Set.of("Venta", "Alquiler").contains(p.get("tipoOperacion").asText()), id);
            assertTrue(Set.of("USD", "PYG").contains(p.get("moneda").asText()), id);
            assertTrue(p.get("precio").isNumber() && p.get("m2").isNumber(), id);
            assertTrue(p.get("barrioId").isInt() && p.get("dormitorios").isInt() && p.get("banos").isInt(), id);
            assertTrue(p.get("fotos").isArray(), id);
            p.get("fotos").forEach(foto -> assertTrue(foto.isTextual(), id));
            if ("Terreno".equals(p.get("categoria").asText())) {
                assertEquals(0, p.get("dormitorios").asInt(), id);
            }
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VERIFICAR_DATOS_DEMO", matches = "(?i)true")
    void losDatosDeDemostracionCoincidenConElGeoJsonOriginal() throws Exception {
        assertTrue(ORIGINAL.exists(), "No se encontró " + ORIGINAL.getAbsolutePath());
        Map<String, JsonNode> originales = new HashMap<>();
        JsonUtil.mapper().readTree(ORIGINAL).get("features")
                .forEach(f -> originales.put(f.get("properties").get("id").asText(), f));

        int comparadas = 0;
        for (JsonNode f : propiedadesDeLaApi().get("features")) {
            JsonNode p = f.get("properties");
            String id = p.get("id").asText();
            JsonNode original = originales.get(id);
            if (original == null) {
                continue; // propiedad creada desde el panel: no existe en el archivo original
            }
            JsonNode o = original.get("properties");
            comparadas++;
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
            for (int i = 0; i < 2; i++) {
                assertEquals(original.get("geometry").get("coordinates").get(i).asDouble(),
                        f.get("geometry").get("coordinates").get(i).asDouble(), 1e-6, id + ".coordenada" + i);
            }
            // Decisión de la Fase 2: las propiedades "Departamento" ahora son "Casa"
            String esperada = "Departamento".equals(o.get("categoria").asText()) ? "Casa" : o.get("categoria").asText();
            assertEquals(esperada, p.get("categoria").asText(), id + ".categoria");
        }
        assertTrue(comparadas > 0, "Ninguna propiedad de la API coincide con el archivo original");
    }

    @Test
    void elDetalleAdministrativoTraeTodoLoNecesarioParaEditar() {
        List<PropiedadResumenDto> listado = admin.listar(PropiedadAdminMapper::resumen);
        if (listado.isEmpty()) {
            return; // base sin propiedades: no hay nada que detallar
        }
        PropiedadResumenDto resumen = listado.get(0);

        PropiedadDetalleDto d = admin.obtener(resumen.id(), PropiedadAdminMapper::detalle);

        assertEquals(resumen.codigo(), d.codigo());
        assertEquals(resumen.ciudad(), d.ubicacion().ciudad());
        assertTrue((d.casa() == null) != (d.terreno() == null), "debe tener exactamente un subtipo");
        assertEquals(resumen.operaciones().size(), d.operaciones().size());
        assertTrue(d.imagenes().stream().filter(PropiedadDetalleDto.ImagenDto::esPrincipal).count() <= 1);
    }

    @Test
    void losCatalogosSeEntreganComoArbol() {
        CatalogosDto c = CatalogoMapper.dto(catalogos.obtener());

        assertEquals(DatosPrueba.numero(dataSource, "SELECT count(*) FROM departamento WHERE EXISTS "
                + "(SELECT 1 FROM ciudad c JOIN barrio b ON b.id_ciudad = c.id_ciudad "
                + "WHERE c.id_departamento = departamento.id_departamento)"), c.departamentos().size());
        long barrios = c.departamentos().stream()
                .flatMap(d -> d.ciudades().stream()).mapToLong(ci -> ci.barrios().size()).sum();
        assertEquals(DatosPrueba.numero(dataSource, "SELECT count(*) FROM barrio"), barrios);
    }
}

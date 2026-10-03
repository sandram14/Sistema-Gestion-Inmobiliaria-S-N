package py.sn.inmobiliaria.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.repository.UbicacionDao;
import py.sn.inmobiliaria.service.CatalogoService;
import py.sn.inmobiliaria.service.PropiedadConsultaService;
import py.sn.inmobiliaria.service.UbicacionService;
import py.sn.inmobiliaria.service.validacion.NormalizadorBusqueda;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static py.sn.inmobiliaria.controller.ServletFalso.get;

/**
 * Prueba los Servlets públicos en memoria (sin Tomcat ni base de datos), con services
 * de prueba que devuelven entidades armadas en memoria.
 */
class ApiPublicaServletTest {

    /** Service de prueba: valida igual que el real y guarda los criterios recibidos. */
    static final class ConsultaFalsa extends PropiedadConsultaService {
        BusquedaRequest ultimosCriterios;

        ConsultaFalsa() {
            super(null, null);
        }

        @Override
        public <T> List<T> buscar(BusquedaRequest criterios, Function<Propiedad, T> conversor) {
            NormalizadorBusqueda.normalizar(criterios); // mismas validaciones que el service real
            ultimosCriterios = criterios;
            return List.of(conversor.apply(Entidades.casaProp001()), conversor.apply(Entidades.terrenoSinDireccion()));
        }

        @Override
        public <T> T obtenerPorCodigo(String codigo, Function<Propiedad, T> conversor) {
            if (!"prop-001".equals(codigo)) {
                throw new NoEncontradoException("La propiedad no existe o ya no está disponible");
            }
            return conversor.apply(Entidades.casaProp001());
        }
    }

    /** DAO geográfico de prueba con una ciudad y un barrio. */
    static final UbicacionDao UBICACION_FALSA = new UbicacionDao() {
        @Override
        public List<CiudadGeo> listarCiudades(Integer departamentoId) {
            if (departamentoId != null && departamentoId != 1) {
                return List.of();
            }
            return List.of(new CiudadGeo(1, "CIUDAD DEL ESTE", "01", 1, "ALTO PARANÁ", -54.641961, -25.505118, 1));
        }

        @Override
        public List<BarrioGeo> listarBarrios(Integer ciudadId) {
            return List.of(new BarrioGeo(10011013, "AREA 1", 1, "CIUDAD DEL ESTE", "ALTO PARANÁ",
                    "{\"type\":\"MultiPolygon\",\"coordinates\":[]}"));
        }

        @Override
        public Optional<Boolean> puntoDentroDeBarrio(int idBarrio, BigDecimal latitud, BigDecimal longitud) {
            return Optional.of(true);
        }

        @Override
        public Optional<FilaUbicacion> barrioQueContiene(BigDecimal latitud, BigDecimal longitud) {
            return Optional.empty();
        }

        @Override
        public List<FilaUbicacion> listarJerarquia() {
            return List.of(new FilaUbicacion(1, "ALTO PARANÁ", 1, "CIUDAD DEL ESTE", 10011013, "AREA 1"));
        }
    };

    static final class CatalogoFalso extends CatalogoService {
        CatalogoFalso() {
            super(null, null, null);
        }

        @Override
        public Catalogos obtener() {
            return new Catalogos(List.of("Casa", "Terreno"), List.of("Disponible"), List.of("Venta", "Alquiler"),
                    List.of("PYG", "USD"), Map.of("Terreno", List.of("Venta")),
                    List.of(new UbicacionDao.FilaUbicacion(1, "ALTO PARANÁ", 1, "CIUDAD DEL ESTE", 10011013, "AREA 1")));
        }
    }

    private ConsultaFalsa consulta;
    private PropiedadServlet propiedades;
    private UbicacionServlet ubicacion;
    private CatalogoServlet catalogo;

    @BeforeEach
    void iniciar() throws Exception {
        consulta = new ConsultaFalsa();
        Map<Class<?>, Object> servicios = Map.of(
                PropiedadConsultaService.class, consulta,
                UbicacionService.class, new UbicacionService(UBICACION_FALSA),
                CatalogoService.class, new CatalogoFalso());
        propiedades = ServletFalso.iniciar(new PropiedadServlet(), servicios);
        ubicacion = ServletFalso.iniciar(new UbicacionServlet(), servicios);
        catalogo = ServletFalso.iniciar(new CatalogoServlet(), servicios);
    }

    // ---- /api/propiedades ----

    @Test
    void listaComoFeatureCollection() throws Exception {
        var r = get(propiedades, "/api/propiedades", null, Map.of());

        assertEquals(200, r.estado);
        assertEquals("application/json", r.cabecera("Content-Type"));
        assertEquals("no-store", r.cabecera("Cache-Control"));
        JsonNode json = r.json();
        assertEquals("FeatureCollection", json.get("type").asText());
        assertEquals(2, json.get("features").size());
        assertEquals("prop-001", json.get("features").get(0).get("properties").get("id").asText());
    }

    @Test
    void pasaLosFiltrosAlServiceYAceptaLosAliasDelFrontend() throws Exception {
        var r = get(propiedades, "/api/propiedades", null, Map.of(
                "ciudadId", "1", "categoria", "Vivienda", "tipoOperacion", "alquiler",
                "presupuestoMax", "5000000", "moneda", "PYG"));

        assertEquals(200, r.estado);
        assertEquals(1, consulta.ultimosCriterios.ciudadId());
        assertEquals("Vivienda", consulta.ultimosCriterios.categoria());
        assertEquals("alquiler", consulta.ultimosCriterios.operacion());
        assertEquals(new BigDecimal("5000000"), consulta.ultimosCriterios.presupuestoMax());
        // Con filtro Alquiler, la tarjeta muestra el precio de alquiler
        JsonNode props = r.json().get("features").get(0).get("properties");
        assertEquals("Alquiler", props.get("tipoOperacion").asText());
        assertEquals("PYG", props.get("moneda").asText());
    }

    @Test
    void filtroPorDepartamento() throws Exception {
        var r = get(propiedades, "/api/propiedades", null, Map.of("departamentoId", "1", "ciudadId", "1"));

        assertEquals(200, r.estado);
        assertEquals(1, consulta.ultimosCriterios.departamentoId());
        assertEquals(400, get(propiedades, "/api/propiedades", null, Map.of("departamentoId", "x")).estado);
        assertEquals(400, get(propiedades, "/api/propiedades", null, Map.of("departamentoId", "0")).estado);
    }

    @Test
    void parametrosConFormatoInvalidoDan400ConElDetallePorCampo() throws Exception {
        var r = get(propiedades, "/api/propiedades", null, Map.of("ciudadId", "abc", "presupuestoMax", "mil"));

        assertEquals(400, r.estado);
        JsonNode error = r.json().get("error");
        assertEquals("VALIDACION", error.get("codigo").asText());
        assertTrue(error.get("campos").has("ciudadId"));
        assertTrue(error.get("campos").has("presupuestoMax"));
    }

    @Test
    void terrenoEnAlquilerDa400() throws Exception {
        var r = get(propiedades, "/api/propiedades", null, Map.of("categoria", "Terreno", "operacion", "Alquiler"));

        assertEquals(400, r.estado);
        assertTrue(r.json().get("error").get("campos").has("operacion"));
    }

    @Test
    void detallePorCodigo() throws Exception {
        var r = get(propiedades, "/api/propiedades", "/prop-001", Map.of());

        assertEquals(200, r.estado);
        assertEquals("Feature", r.json().get("type").asText());
        assertEquals("Casa en Area 1", r.json().get("properties").get("titulo").asText());
    }

    @Test
    void codigoInexistenteOConFormatoRaroDa404() throws Exception {
        for (String ruta : List.of("/prop-999", "/../../etc/passwd", "/prop-001/fotos", "/1")) {
            var r = get(propiedades, "/api/propiedades", ruta, Map.of());
            assertEquals(404, r.estado, ruta);
            assertEquals("NO_ENCONTRADO", r.json().get("error").get("codigo").asText(), ruta);
        }
    }

    @Test
    void metodoNoPermitidoDa405EnJson() throws Exception {
        var r = ServletFalso.ejecutar(propiedades, "POST", "/api/propiedades", null, Map.of());

        assertEquals(405, r.estado);
        assertEquals("METODO_NO_PERMITIDO", r.json().get("error").get("codigo").asText());
    }

    @Test
    void sinBaseConfiguradaDa503() throws Exception {
        PropiedadServlet sinServicios = ServletFalso.iniciar(new PropiedadServlet(), Map.of());

        var r = get(sinServicios, "/api/propiedades", null, Map.of());

        assertEquals(503, r.estado);
        assertEquals("SERVICIO_NO_DISPONIBLE", r.json().get("error").get("codigo").asText());
    }

    // ---- /api/ciudades y /api/barrios ----

    @Test
    void ciudadesConCacheDeUnaHora() throws Exception {
        var r = get(ubicacion, "/api/ciudades", null, Map.of());

        assertEquals(200, r.estado);
        assertEquals("public, max-age=3600", r.cabecera("Cache-Control"));
        assertEquals("1", r.json().get("features").get(0).get("properties").get("id").asText());
    }

    @Test
    void ciudadesPorDepartamento() throws Exception {
        assertEquals(1, get(ubicacion, "/api/ciudades", null, Map.of("departamentoId", "1")).json().get("features").size());
        assertEquals(0, get(ubicacion, "/api/ciudades", null, Map.of("departamentoId", "2")).json().get("features").size());
        assertEquals(400, get(ubicacion, "/api/ciudades", null, Map.of("departamentoId", "abc")).estado);
        assertEquals(400, get(ubicacion, "/api/ciudades", null, Map.of("departamentoId", "-1")).estado);
    }

    @Test
    void barriosConGeometriaComoObjeto() throws Exception {
        var r = get(ubicacion, "/api/barrios", null, Map.of("ciudadId", "1"));

        assertEquals(200, r.estado);
        JsonNode f = r.json().get("features").get(0);
        assertEquals("MultiPolygon", f.get("geometry").get("type").asText());
        assertEquals("1", f.get("properties").get("distritoCod").asText());
    }

    @Test
    void barriosDeUnaCiudadInexistenteDa404() throws Exception {
        var r = get(ubicacion, "/api/barrios", null, Map.of("ciudadId", "99"));

        assertEquals(404, r.estado);
        assertEquals("no-store", r.cabecera("Cache-Control")); // los errores nunca se guardan en caché
    }

    // ---- /api/catalogos ----

    @Test
    void catalogosComoArbol() throws Exception {
        var r = get(catalogo, "/api/catalogos", null, Map.of());

        assertEquals(200, r.estado);
        JsonNode json = r.json();
        assertEquals("Casa", json.get("tipos").get(0).asText());
        assertEquals("AREA 1", json.get("departamentos").get(0).get("ciudades").get(0).get("barrios").get(0).get("nombre").asText());
        assertEquals("Venta", json.get("operacionesPorTipo").get("Terreno").get(0).asText());
    }
}

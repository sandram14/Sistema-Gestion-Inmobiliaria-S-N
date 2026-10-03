package py.sn.inmobiliaria.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.controller.ServletFalso;
import py.sn.inmobiliaria.controller.ServletFalso.Peticion;
import py.sn.inmobiliaria.repository.UbicacionDao;
import py.sn.inmobiliaria.service.UbicacionService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GET /api/admin/ubicacion en memoria: Servlet y UbicacionService reales, con un DAO de prueba
 * que "contiene" un único rectángulo como barrio.
 */
class UbicacionAdminServletTest {

    /** DAO de prueba: un barrio que cubre latitud -25.6..-25.4 y longitud -54.7..-54.5. */
    static final class DaoFalso implements UbicacionDao {
        BigDecimal ultimaLatitud;
        BigDecimal ultimaLongitud;

        @Override
        public Optional<FilaUbicacion> barrioQueContiene(BigDecimal latitud, BigDecimal longitud) {
            ultimaLatitud = latitud;
            ultimaLongitud = longitud;
            boolean dentro = latitud.compareTo(new BigDecimal("-25.6")) >= 0 && latitud.compareTo(new BigDecimal("-25.4")) <= 0
                    && longitud.compareTo(new BigDecimal("-54.7")) >= 0 && longitud.compareTo(new BigDecimal("-54.5")) <= 0;
            return dentro ? Optional.of(new FilaUbicacion(1, "ALTO PARANÁ", 1, "CIUDAD DEL ESTE", 10011013, "AREA 1"))
                    : Optional.empty();
        }

        @Override
        public List<CiudadGeo> listarCiudades(Integer departamentoId) {
            return List.of();
        }

        @Override
        public List<BarrioGeo> listarBarrios(Integer ciudadId) {
            return List.of();
        }

        @Override
        public Optional<Boolean> puntoDentroDeBarrio(int idBarrio, BigDecimal latitud, BigDecimal longitud) {
            return Optional.empty();
        }

        @Override
        public List<FilaUbicacion> listarJerarquia() {
            return List.of();
        }
    }

    private DaoFalso dao;
    private UbicacionAdminServlet servlet;

    @BeforeEach
    void iniciar() throws Exception {
        dao = new DaoFalso();
        servlet = ServletFalso.iniciar(new UbicacionAdminServlet(), Map.of(UbicacionService.class, new UbicacionService(dao)));
    }

    private ServletFalso.Respuesta get(String latitud, String longitud) throws Exception {
        Map<String, String> p = new HashMap<>();
        if (latitud != null) {
            p.put("latitud", latitud);
        }
        if (longitud != null) {
            p.put("longitud", longitud);
        }
        return ServletFalso.ejecutar(servlet, Peticion.de("GET", "/api/admin/ubicacion", null).parametros(p));
    }

    @Test
    void puntoDentroDeUnBarrioDevuelveLaJerarquiaCompleta() throws Exception {
        var r = get("-25.5117004999", "-54.6118");

        assertEquals(200, r.estado);
        JsonNode json = r.json();
        assertEquals("ALTO PARANÁ", json.get("departamento").get("nombre").asText());
        assertEquals(1, json.get("departamento").get("id").asInt());
        assertEquals("CIUDAD DEL ESTE", json.get("ciudad").get("nombre").asText());
        assertEquals(10011013, json.get("barrio").get("id").asInt());
        assertEquals("AREA 1", json.get("barrio").get("nombre").asText());
        // Coordenadas redondeadas a 6 decimales, igual que al guardar la propiedad
        assertEquals("-25.5117", json.get("latitud").toString().replaceAll("0+$", ""));
        assertEquals(new BigDecimal("-25.511700"), dao.ultimaLatitud);
        assertEquals(new BigDecimal("-54.6118").setScale(6, RoundingMode.HALF_UP), dao.ultimaLongitud);
    }

    @Test
    void puntoFueraDeLasZonasDevuelveUnErrorClaro() throws Exception {
        var r = get("0", "0");

        assertEquals(404, r.estado);
        JsonNode error = r.json().get("error");
        assertEquals("FUERA_DE_ZONA", error.get("codigo").asText());
        assertTrue(error.get("mensaje").asText().contains("fuera de las zonas registradas"));
    }

    @Test
    void faltanCoordenadas() throws Exception {
        var r = get(null, null);

        assertEquals(400, r.estado);
        JsonNode campos = r.json().get("error").get("campos");
        assertTrue(campos.has("latitud"));
        assertTrue(campos.has("longitud"));
    }

    @Test
    void coordenadasQueNoSonNumeros() throws Exception {
        var r = get("abc", "-54,6"); // la coma decimal no es válida en la URL

        assertEquals(400, r.estado);
        assertTrue(r.json().get("error").get("campos").has("latitud"));
        assertTrue(r.json().get("error").get("campos").has("longitud"));
    }

    @Test
    void coordenadasFueraDeRango() throws Exception {
        var r = get("95", "-200");

        assertEquals(400, r.estado);
        assertTrue(r.json().get("error").get("campos").has("latitud"));
        assertFalse(r.json().get("error").has("detalle")); // nunca detalles internos
    }

    @Test
    void soloAdmiteGet() throws Exception {
        var r = ServletFalso.ejecutar(servlet, Peticion.de("POST", "/api/admin/ubicacion", null).json("{}"));

        assertEquals(405, r.estado);
    }
}

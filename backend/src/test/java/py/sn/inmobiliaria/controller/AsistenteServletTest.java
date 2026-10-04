package py.sn.inmobiliaria.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.config.AsistenteConfig;
import py.sn.inmobiliaria.controller.ServletFalso.Peticion;
import py.sn.inmobiliaria.dto.response.AsistenteResponse;
import py.sn.inmobiliaria.service.asistente.AsistenteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenteServletTest {

    /** Service de prueba: guarda la sesión recibida y responde un texto fijo. */
    static final class AsistenteFalso extends AsistenteService {
        final List<UUID> sesiones = new ArrayList<>();
        String ultimoCodigo;

        AsistenteFalso() {
            super(null, null, null, null, null, AsistenteConfig.desde(Map.of()));
        }

        @Override
        public AsistenteResponse responder(UUID sesion, String pregunta, String codigoPropiedad) {
            sesiones.add(sesion);
            ultimoCodigo = codigoPropiedad;
            return new AsistenteResponse("Hola, " + pregunta, false, List.of());
        }
    }

    private AsistenteFalso asistente;
    private AsistenteServlet servlet;

    @BeforeEach
    void iniciar() throws Exception {
        asistente = new AsistenteFalso();
        servlet = ServletFalso.iniciar(new AsistenteServlet(), Map.of(
                AsistenteService.class, asistente,
                AppConfig.class, AppConfig.desde(Map.of(), new Properties())));
    }

    private static Peticion consulta(String json) {
        return Peticion.de("POST", "/api/asistente/consultas", null).json(json);
    }

    @Test
    void primeraConsultaCreaLaCookieAnonimaDelVisitante() throws Exception {
        var r = ServletFalso.ejecutar(servlet, consulta("{\"pregunta\":\"¿Hay casas?\",\"codigoPropiedad\":\"prop-001\"}"));

        assertEquals(200, r.estado);
        assertEquals("Hola, ¿Hay casas?", r.json().get("respuesta").asText());
        assertEquals("prop-001", asistente.ultimoCodigo);
        assertEquals(1, r.cookies.size());
        Cookie c = r.cookies.get(0);
        assertEquals(AsistenteServlet.COOKIE_SESION, c.getName());
        assertEquals(asistente.sesiones.get(0).toString(), c.getValue());
        assertTrue(c.isHttpOnly());
        assertEquals("/", c.getPath());
        assertEquals("Lax", c.getAttribute("SameSite"));
        assertFalse(c.getSecure()); // en desarrollo; en producción se marca Secure
    }

    @Test
    void lasSiguientesConsultasReutilizanLaMismaSesion() throws Exception {
        UUID existente = UUID.randomUUID();

        var r = ServletFalso.ejecutar(servlet, consulta("{\"pregunta\":\"Hola\"}")
                .cookie(AsistenteServlet.COOKIE_SESION, existente.toString()));

        assertEquals(existente, asistente.sesiones.get(0));
        assertTrue(r.cookies.isEmpty()); // no se crea otra
    }

    @Test
    void unaCookieManipuladaSeReemplaza() throws Exception {
        var r = ServletFalso.ejecutar(servlet, consulta("{\"pregunta\":\"Hola\"}")
                .cookie(AsistenteServlet.COOKIE_SESION, "no-es-un-uuid"));

        assertEquals(1, r.cookies.size());
        assertNotEquals("no-es-un-uuid", r.cookies.get(0).getValue());
    }

    @Test
    void cuerpoInvalidoYMetodosNoPermitidos() throws Exception {
        assertEquals(400, ServletFalso.ejecutar(servlet, consulta("{\"pregunta\":\"x\",\"apiKey\":\"y\"}")).estado);
        assertEquals(415, ServletFalso.ejecutar(servlet,
                Peticion.de("POST", "/api/asistente/consultas", null).cuerpo("pregunta=x", "text/plain")).estado);
        assertEquals(405, ServletFalso.ejecutar(servlet, Peticion.de("GET", "/api/asistente/consultas", null)).estado);
        assertTrue(asistente.sesiones.isEmpty());
    }
}

package py.sn.inmobiliaria.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.controller.ServletFalso;
import py.sn.inmobiliaria.controller.ServletFalso.Navegador;
import py.sn.inmobiliaria.controller.ServletFalso.Peticion;
import py.sn.inmobiliaria.filter.AuthFilter;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.AuthService;
import py.sn.inmobiliaria.service.AuthServiceTest;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flujo completo de autenticación en memoria: AuthFilter + AuthServlet, con una sesión
 * que se conserva entre peticiones como lo haría la cookie del navegador.
 */
class AutenticacionTest {

    private static final String LOGIN_OK = "{\"email\":\"admin@sn.local\",\"password\":\"Clave-Correcta\"}";

    private AuthServlet servlet;
    private AuthFilter filtro;
    private Navegador navegador;
    private AuthServiceTest.DaoFalso dao;

    @BeforeEach
    void iniciar() throws Exception {
        AppConfig config = AppConfig.desde(Map.of("APP_ORIGENES_PERMITIDOS", "http://localhost:5174"), new Properties());
        dao = new AuthServiceTest.DaoFalso();
        dao.usuarios.put(8, AuthServiceTest.conRol(8, "gerente@sn.local", "GERENTE"));
        dao.usuarios.put(9, AuthServiceTest.conRol(9, "vendedor@sn.local", "VENDEDOR"));
        AuthService auth = AuthServiceTest.nuevoServicio(dao);
        servlet = ServletFalso.iniciar(new AuthServlet(), Map.of(AuthService.class, auth));
        // El filtro relee al usuario en cada petición: usa el mismo servicio (y la misma "base")
        filtro = ServletFalso.iniciarFiltro(new AuthFilter(), Map.of(AppConfig.class, config, AuthService.class, auth));
        navegador = new Navegador();
    }

    private Peticion login(String json) {
        return Peticion.de("POST", "/api/admin/auth", "/login").json(json).navegador(navegador);
    }

    private Peticion me() {
        return Peticion.de("GET", "/api/admin/auth", "/me").navegador(navegador);
    }

    // ---- AuthServlet ----

    @Test
    void loginCorrectoCreaLaSesionYDevuelveElUsuario() throws Exception {
        var r = ServletFalso.ejecutar(servlet, login(LOGIN_OK));

        assertEquals(200, r.estado);
        JsonNode usuario = r.json().get("usuario");
        assertEquals("admin@sn.local", usuario.get("email").asText());
        assertFalse(usuario.has("password"));
        assertFalse(usuario.has("passwordHash"));
        assertNotNull(navegador.sesion());
        assertTrue(navegador.sesion().atributos.get(UsuarioSesion.ATRIBUTO) instanceof UsuarioSesion);
    }

    @Test
    void cadaLoginUsaUnaSesionNueva() throws Exception {
        String idAnterior = navegador.crearSesionAnonima().id;

        ServletFalso.ejecutar(servlet, login(LOGIN_OK));

        assertNotEquals(idAnterior, navegador.sesion().id); // protección contra fijación de sesión
        assertNotNull(navegador.sesion().atributos.get(UsuarioSesion.ATRIBUTO));
    }

    @Test
    void credencialesIncorrectasDan401SinCrearSesion() throws Exception {
        var r = ServletFalso.ejecutar(servlet, login("{\"email\":\"admin@sn.local\",\"password\":\"mal\"}"));

        assertEquals(401, r.estado);
        assertEquals("NO_AUTENTICADO", r.json().get("error").get("codigo").asText());
        assertNull(navegador.sesion());
    }

    @Test
    void cuerpoInvalidoDa400Y415() throws Exception {
        assertEquals(400, ServletFalso.ejecutar(servlet, login("{no es json")).estado);
        var extra = ServletFalso.ejecutar(servlet, login("{\"email\":\"a\",\"password\":\"b\",\"rol\":\"ADMIN\"}"));
        assertEquals(400, extra.estado);
        assertTrue(extra.json().get("error").get("campos").has("rol"));
        var texto = ServletFalso.ejecutar(servlet,
                Peticion.de("POST", "/api/admin/auth", "/login").cuerpo(LOGIN_OK, "text/plain").navegador(navegador));
        assertEquals(415, texto.estado);
    }

    @Test
    void meYLogout() throws Exception {
        assertEquals(401, ServletFalso.ejecutar(servlet, me()).estado);

        ServletFalso.ejecutar(servlet, login(LOGIN_OK));
        var conSesion = ServletFalso.ejecutar(servlet, me());
        assertEquals(200, conSesion.estado);
        assertEquals("Sandra", conSesion.json().get("usuario").get("nombre").asText());

        var logout = ServletFalso.ejecutar(servlet, Peticion.de("POST", "/api/admin/auth", "/logout").navegador(navegador));
        assertEquals(204, logout.estado);
        assertEquals(401, ServletFalso.ejecutar(servlet, me()).estado);
    }

    // ---- AuthFilter ----

    @Test
    void sinSesionLasRutasAdministrativasDan401() throws Exception {
        var r = ServletFalso.filtrar(filtro, Peticion.de("GET", "/api/admin/propiedades", null).navegador(navegador));

        assertFalse(r.continuo());
        assertEquals(401, r.respuesta().estado);
        assertEquals("NO_AUTENTICADO", r.respuesta().json().get("error").get("codigo").asText());
    }

    @Test
    void laDeteccionDeUbicacionRequiereSesion() throws Exception {
        var sinSesion = ServletFalso.filtrar(filtro, Peticion.de("GET", "/api/admin/ubicacion", null)
                .parametros(Map.of("latitud", "-25.5", "longitud", "-54.6")).navegador(navegador));
        assertFalse(sinSesion.continuo());
        assertEquals(401, sinSesion.respuesta().estado);

        ServletFalso.ejecutar(servlet, login(LOGIN_OK));
        assertTrue(ServletFalso.filtrar(filtro, Peticion.de("GET", "/api/admin/ubicacion", null)
                .navegador(navegador)).continuo());
    }

    @Test
    void conSesionDeAdministradorDejaPasar() throws Exception {
        ServletFalso.ejecutar(servlet, login(LOGIN_OK));

        var r = ServletFalso.filtrar(filtro, Peticion.de("GET", "/api/admin/propiedades", null).navegador(navegador));

        assertTrue(r.continuo());
    }

    private Peticion loginComo(String email) {
        return login("{\"email\":\"" + email + "\",\"password\":\"Clave-Correcta\"}");
    }

    private int estado(String metodo, String ruta) throws Exception {
        var r = ServletFalso.filtrar(filtro, Peticion.de(metodo, ruta, null).navegador(navegador));
        return r.continuo() ? 0 : r.respuesta().estado; // 0 = pasó el filtro
    }

    @Test
    void elVendedorNoPuedeDarDeBajaNiVerRutasSinRegla() throws Exception {
        assertEquals(200, ServletFalso.ejecutar(servlet, loginComo("vendedor@sn.local")).estado);

        assertEquals(0, estado("GET", "/api/admin/propiedades"));
        assertEquals(0, estado("POST", "/api/admin/propiedades"));
        assertEquals(403, estado("DELETE", "/api/admin/propiedades/5"));
        assertEquals(0, estado("GET", "/api/admin/auth/me"));
    }

    @Test
    void elGerentePuedeDarDeBaja() throws Exception {
        ServletFalso.ejecutar(servlet, loginComo("gerente@sn.local"));

        assertEquals(0, estado("DELETE", "/api/admin/propiedades/5"));
    }

    @Test
    void unaRutaAdministrativaSinReglaQuedaBloqueadaInclusoParaElAdministrador() throws Exception {
        ServletFalso.ejecutar(servlet, login(LOGIN_OK));

        assertEquals(403, estado("GET", "/api/admin/ruta-nueva-sin-regla"));
        assertEquals(403, estado("PUT", "/api/admin/propiedades"));   // método sin regla
    }

    @Test
    void unaCuentaDesactivadaPierdeElAccesoEnLaSiguientePeticion() throws Exception {
        ServletFalso.ejecutar(servlet, login(LOGIN_OK));
        assertEquals(0, estado("GET", "/api/admin/propiedades"));

        dao.usuarios.get(7).setActivo(false);

        assertEquals(401, estado("GET", "/api/admin/propiedades"));
        assertTrue(navegador.sesion() == null || navegador.sesion().atributos.isEmpty()); // sesión invalidada
    }

    @Test
    void unCambioDeRolSeAplicaEnLaSiguientePeticion() throws Exception {
        ServletFalso.ejecutar(servlet, login(LOGIN_OK));
        assertEquals(0, estado("DELETE", "/api/admin/propiedades/5"));

        dao.usuarios.get(7).setRol(py.sn.inmobiliaria.prueba.Entidades.crear(
                py.sn.inmobiliaria.model.Rol.class, Map.of("id", 3, "nombre", "VENDEDOR")));

        assertEquals(403, estado("DELETE", "/api/admin/propiedades/5"));
        var enSesion = (UsuarioSesion) navegador.sesion().atributos.get(UsuarioSesion.ATRIBUTO);
        assertEquals("VENDEDOR", enSesion.rol());                        // la sesión quedó actualizada
    }

    @Test
    void loginYLogoutNoRequierenSesion() throws Exception {
        assertTrue(ServletFalso.filtrar(filtro, login(LOGIN_OK)).continuo());
        assertTrue(ServletFalso.filtrar(filtro,
                Peticion.de("POST", "/api/admin/auth", "/logout").navegador(navegador)).continuo());
    }

    @Test
    void rechazaPeticionesDeOtroSitio() throws Exception {
        var otroSitio = ServletFalso.filtrar(filtro, login(LOGIN_OK)
                .cabecera("Origin", "https://sitio-malicioso.com").cabecera("Host", "localhost:8080"));
        assertEquals(403, otroSitio.respuesta().estado);
        assertFalse(otroSitio.continuo());

        var mismoSitio = ServletFalso.filtrar(filtro, login(LOGIN_OK)
                .cabecera("Origin", "http://localhost:5173").cabecera("Host", "localhost:5173"));
        assertTrue(mismoSitio.continuo());

        var permitido = ServletFalso.filtrar(filtro, login(LOGIN_OK)
                .cabecera("Origin", "http://localhost:5174").cabecera("Host", "localhost:8080"));
        assertTrue(permitido.continuo());
    }

    @Test
    void deleteYPatchSinCuerpoPasanConSesion() throws Exception {
        ServletFalso.ejecutar(servlet, login(LOGIN_OK));

        assertTrue(ServletFalso.filtrar(filtro,
                Peticion.de("DELETE", "/api/admin/propiedades", "/1").navegador(navegador)).continuo());
        assertTrue(ServletFalso.filtrar(filtro,
                Peticion.de("PATCH", "/api/admin/propiedades", "/1/imagenes/5/principal").navegador(navegador)).continuo());
    }

    @Test
    void rechazaFormulariosHtmlComoCuerpo() throws Exception {
        var formulario = ServletFalso.filtrar(filtro, Peticion.de("POST", "/api/admin/auth", "/login")
                .cuerpo("email=a&password=b", "application/x-www-form-urlencoded").navegador(navegador));

        assertEquals(415, formulario.respuesta().estado);
        assertFalse(formulario.continuo());
    }
}

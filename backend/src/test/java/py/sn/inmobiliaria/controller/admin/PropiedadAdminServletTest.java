package py.sn.inmobiliaria.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import py.sn.inmobiliaria.controller.ServletFalso;
import py.sn.inmobiliaria.controller.ServletFalso.Peticion;
import py.sn.inmobiliaria.controller.UploadsServlet;
import py.sn.inmobiliaria.dto.request.ImagenRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.service.AlmacenImagenes;
import py.sn.inmobiliaria.service.AlmacenImagenesTest;
import py.sn.inmobiliaria.service.PropiedadAdminService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRUD administrativo e imágenes, en memoria: Servlet real + almacén de archivos real
 * (en una carpeta temporal) + service de prueba que registra las llamadas.
 */
class PropiedadAdminServletTest {

    /** Service de prueba: una sola propiedad (id 1) y registro de lo que se le pide. */
    static final class AdminFalso extends PropiedadAdminService {
        final List<String> llamadas = new ArrayList<>();
        PropiedadRequest ultimaRequest;
        ImagenRequest ultimaImagen;
        boolean fallarAlRegistrarImagen;
        String urlAEliminar = "/images/propiedades/prop-001/1.png";

        AdminFalso() {
            super(null, null, null, null, null);
        }

        private static void existe(long id) {
            if (id != 1) {
                throw new NoEncontradoException("La propiedad no existe");
            }
        }

        @Override
        public <T> List<T> listar(Function<Propiedad, T> conversor) {
            return List.of(conversor.apply(Entidades.casaProp001()));
        }

        @Override
        public <T> T obtener(long id, Function<Propiedad, T> conversor) {
            existe(id);
            return conversor.apply(Entidades.casaProp001());
        }

        Integer ultimoResponsable;
        boolean ultimoCambiaResponsable;

        /** La propiedad 1 está asignada al vendedor con id 50. */
        @Override
        public void verificarPuedeEditar(long id, py.sn.inmobiliaria.seguridad.UsuarioSesion actor) {
            if (!actor.tiene(py.sn.inmobiliaria.seguridad.Permiso.PROPIEDADES_EDITAR_TODAS) && actor.id() != 50) {
                throw new py.sn.inmobiliaria.exception.AccesoDenegadoException("Esta propiedad está asignada a otro responsable");
            }
        }

        @Override
        public PropiedadGuardada crear(PropiedadRequest req, Integer idResponsable) {
            llamadas.add("crear");
            ultimaRequest = req;
            ultimoResponsable = idResponsable;
            return new PropiedadGuardada(1, "prop-001");
        }

        @Override
        public PropiedadGuardada actualizar(long id, PropiedadRequest req, boolean cambiarResponsable, Integer idResponsable) {
            existe(id);
            ultimoCambiaResponsable = cambiarResponsable;
            ultimoResponsable = idResponsable;
            llamadas.add("actualizar " + id);
            ultimaRequest = req;
            return new PropiedadGuardada(id, "prop-001");
        }

        @Override
        public void eliminar(long id) {
            existe(id);
            llamadas.add("eliminar " + id);
        }

        @Override
        public long agregarImagen(long idPropiedad, ImagenRequest req) {
            existe(idPropiedad);
            if (fallarAlRegistrarImagen) {
                throw new IllegalStateException("falla simulada de la base");
            }
            ultimaImagen = req;
            return 500;
        }

        @Override
        public String eliminarImagen(long idPropiedad, long idImagen) {
            llamadas.add("eliminarImagen " + idImagen);
            return urlAEliminar;
        }

        @Override
        public void marcarPrincipal(long idPropiedad, long idImagen) {
            llamadas.add("principal " + idImagen);
        }
    }

    private static final String PROPIEDAD_JSON = """
            {"titulo":"Casa nueva","latitud":-25.51,"longitud":-54.61,"idBarrio":10011013,"tipo":"Casa",
             "casa":{"habitaciones":2,"banos":1,"superficieConstruida":90},
             "operaciones":[{"operacion":"Venta","precio":850000000}]}
            """;

    @TempDir
    Path carpeta;

    private AdminFalso admin;
    private AlmacenImagenes almacen;
    private PropiedadAdminServlet servlet;

    @BeforeEach
    void iniciar() throws Exception {
        admin = new AdminFalso();
        almacen = new AlmacenImagenes(carpeta);
        servlet = ServletFalso.iniciar(new PropiedadAdminServlet(),
                Map.of(PropiedadAdminService.class, admin, AlmacenImagenes.class, almacen));
    }

    private ServletFalso.Respuesta ejecutar(Peticion p) throws Exception {
        return ServletFalso.ejecutar(servlet, p);
    }

    private static Peticion pet(String metodo, String ruta) {
        return Peticion.de(metodo, "/api/admin/propiedades", ruta);
    }

    private long archivosGuardados() throws Exception {
        try (Stream<Path> s = Files.walk(carpeta)) {
            return s.filter(Files::isRegularFile).count();
        }
    }

    // ---- CRUD ----

    @Test
    void listadoYDetalle() throws Exception {
        var lista = ejecutar(pet("GET", null));
        assertEquals(200, lista.estado);
        assertEquals("prop-001", lista.json().get(0).get("codigo").asText());

        var detalle = ejecutar(pet("GET", "/1"));
        assertEquals(200, detalle.estado);
        assertEquals("Casa", detalle.json().get("tipo").asText());
        assertEquals(3, detalle.json().get("imagenes").size());
    }

    @Test
    void idsInexistentesOInvalidos() throws Exception {
        assertEquals(404, ejecutar(pet("GET", "/99")).estado);
        assertEquals(404, ejecutar(pet("GET", "/abc")).estado);
        assertEquals(404, ejecutar(pet("GET", "/1/otra-cosa")).estado);
    }

    @Test
    void crearDevuelve201ConLocationYElDetalle() throws Exception {
        var r = ejecutar(pet("POST", null).json(PROPIEDAD_JSON));

        assertEquals(201, r.estado);
        assertEquals("/api/admin/propiedades/1", r.cabecera("Location"));
        assertEquals("prop-001", r.json().get("codigo").asText());
        assertEquals("Casa nueva", admin.ultimaRequest.titulo());
        assertEquals(1, admin.ultimaRequest.operaciones().size());
    }

    @Test
    void crearConJsonInvalidoOCampoDesconocido() throws Exception {
        assertEquals(400, ejecutar(pet("POST", null).json("{\"titulo\":")).estado);
        var extra = ejecutar(pet("POST", null).json("{\"titulo\":\"x\",\"codigo\":\"prop-999\"}"));
        assertEquals(400, extra.estado);
        assertTrue(extra.json().get("error").get("campos").has("codigo")); // el código no se puede elegir
    }

    @Test
    void actualizarYEliminar() throws Exception {
        assertEquals(200, ejecutar(pet("PUT", "/1").json(PROPIEDAD_JSON)).estado);
        assertEquals(204, ejecutar(pet("DELETE", "/1")).estado);
        assertEquals(List.of("actualizar 1", "eliminar 1"), admin.llamadas);
        assertEquals(404, ejecutar(pet("DELETE", "/99")).estado);
    }

    @Test
    void metodosNoPermitidos() throws Exception {
        assertEquals(405, ejecutar(pet("PUT", null).json(PROPIEDAD_JSON)).estado);  // PUT a la colección
        assertEquals(405, ejecutar(pet("PATCH", "/1")).estado);
        assertEquals(405, ejecutar(pet("GET", "/1/imagenes/5")).estado);
    }

    // ---- Imágenes ----

    @Test
    void subeUnaImagenLaGuardaYLaRegistra() throws Exception {
        var r = ejecutar(pet("POST", "/1/imagenes")
                .parte("archivo", AlmacenImagenesTest.JPEG)
                .parametros(Map.of("descripcion", "Fachada")));

        assertEquals(201, r.estado);
        String url = admin.ultimaImagen.url();
        assertTrue(url.matches("^/uploads/prop-001/[0-9a-f-]{36}\\.jpg$"), url);
        assertEquals("Fachada", admin.ultimaImagen.descripcion());
        assertArrayEquals(AlmacenImagenesTest.JPEG, Files.readAllBytes(almacen.resolver(url).orElseThrow()));
    }

    @Test
    void unArchivoQueNoEsImagenSeRechazaSinGuardarNada() throws Exception {
        var r = ejecutar(pet("POST", "/1/imagenes").parte("archivo", "hola".getBytes(StandardCharsets.UTF_8)));

        assertEquals(400, r.estado);
        assertTrue(r.json().get("error").get("campos").has("archivo"));
        assertEquals(0, archivosGuardados());
    }

    @Test
    void siFallaElRegistroEnLaBaseNoQuedaUnArchivoHuerfano() throws Exception {
        admin.fallarAlRegistrarImagen = true;

        var r = ejecutar(pet("POST", "/1/imagenes").parte("archivo", AlmacenImagenesTest.PNG));

        assertEquals(500, r.estado);
        assertEquals(0, archivosGuardados());
    }

    @Test
    void validacionesDeLaSubida() throws Exception {
        assertEquals(415, ejecutar(pet("POST", "/1/imagenes").json("{}")).estado);
        assertEquals(400, ejecutar(pet("POST", "/1/imagenes").parte("otroCampo", AlmacenImagenesTest.PNG)).estado);
        assertEquals(404, ejecutar(pet("POST", "/99/imagenes").parte("archivo", AlmacenImagenesTest.PNG)).estado);
        assertEquals(0, archivosGuardados());
    }

    @Test
    void eliminarImagenBorraTambienElArchivoSubido() throws Exception {
        String url = almacen.guardar("prop-001", new java.io.ByteArrayInputStream(AlmacenImagenesTest.WEBP));
        admin.urlAEliminar = url;

        var r = ejecutar(pet("DELETE", "/1/imagenes/5"));

        assertEquals(200, r.estado);
        assertEquals(List.of("eliminarImagen 5"), admin.llamadas);
        assertFalse(Files.exists(almacen.resolver(url).orElseThrow()));
    }

    @Test
    void marcarPrincipal() throws Exception {
        var r = ejecutar(pet("PATCH", "/1/imagenes/7/principal"));

        assertEquals(200, r.estado);
        assertEquals(List.of("principal 7"), admin.llamadas);
        JsonNode imagenes = r.json().get("imagenes");
        assertTrue(imagenes.isArray());
    }

    // ---- /uploads ----

    @Test
    void sirveLasImagenesSubidas() throws Exception {
        UploadsServlet uploads = ServletFalso.iniciar(new UploadsServlet(), Map.of(AlmacenImagenes.class, almacen));
        String url = almacen.guardar("prop-001", new java.io.ByteArrayInputStream(AlmacenImagenesTest.PNG));

        var r = ServletFalso.ejecutar(uploads, Peticion.de("GET", "/uploads", url.substring("/uploads".length())));

        assertEquals(200, r.estado);
        assertEquals("image/png", r.cabecera("Content-Type"));
        assertEquals("public, max-age=31536000, immutable", r.cabecera("Cache-Control"));
        assertArrayEquals(AlmacenImagenesTest.PNG, r.cuerpo.toByteArray());
    }

    @Test
    void uploadsNoPermiteSalirDeLaCarpeta() throws Exception {
        UploadsServlet uploads = ServletFalso.iniciar(new UploadsServlet(), Map.of(AlmacenImagenes.class, almacen));
        Files.writeString(carpeta.resolve("secreto.txt"), "no debe verse");

        for (String ruta : List.of("/../secreto.txt", "/secreto.txt", "/prop-001/no-existe.png",
                "/prop-001/00000000-0000-0000-0000-000000000000.png")) {
            var r = ServletFalso.ejecutar(uploads, Peticion.de("GET", "/uploads", ruta));
            assertEquals(404, r.estado, ruta);
        }
    }

    // ---- Etapa 2B: responsable y control de propiedad ----

    private static py.sn.inmobiliaria.seguridad.UsuarioSesion vendedor(int id) {
        return new py.sn.inmobiliaria.seguridad.UsuarioSesion(id, "Ven", "Dedor", "v@x.com", "VENDEDOR");
    }

    @Test
    void sinUsuarioActualLaPeticionSeRechaza() throws Exception {
        var r = ServletFalso.ejecutar(servlet, Peticion.de("PUT", "/api/admin/propiedades", "/1").json(PROPIEDAD_JSON).actor(null));
        assertEquals(401, r.estado);
        assertTrue(admin.llamadas.isEmpty());
    }

    @Test
    void loQueCreaUnVendedorQuedaAsignadoAEl() throws Exception {
        String json = PROPIEDAD_JSON.replace("\"tipo\":\"Casa\",", "\"tipo\":\"Casa\",\"idResponsable\":99,");
        ServletFalso.ejecutar(servlet, Peticion.de("POST", "/api/admin/propiedades", null).json(json).actor(vendedor(50)));
        assertEquals(50, admin.ultimoResponsable);                 // ignora el idResponsable que envió
    }

    @Test
    void elAdministradorEligeElResponsableAlCrearYEditar() throws Exception {
        String json = PROPIEDAD_JSON.replace("\"tipo\":\"Casa\",", "\"tipo\":\"Casa\",\"idResponsable\":7,");
        ServletFalso.ejecutar(servlet, Peticion.de("POST", "/api/admin/propiedades", null).json(json));
        assertEquals(7, admin.ultimoResponsable);
        ServletFalso.ejecutar(servlet, Peticion.de("PUT", "/api/admin/propiedades", "/1").json(json));
        assertTrue(admin.ultimoCambiaResponsable);
        assertEquals(7, admin.ultimoResponsable);
    }

    @Test
    void unVendedorEditaSuPropiedadPeroNoCambiaElResponsable() throws Exception {
        var r = ServletFalso.ejecutar(servlet, Peticion.de("PUT", "/api/admin/propiedades", "/1").json(PROPIEDAD_JSON).actor(vendedor(50)));
        assertEquals(200, r.estado);
        assertFalse(admin.ultimoCambiaResponsable);
    }

    @Test
    void unVendedorNoPuedeEditarNiTocarFotosDePropiedadesAjenas() throws Exception {
        assertEquals(403, ServletFalso.ejecutar(servlet,
                Peticion.de("PUT", "/api/admin/propiedades", "/1").json(PROPIEDAD_JSON).actor(vendedor(51))).estado);
        assertEquals(403, ServletFalso.ejecutar(servlet,
                Peticion.de("PATCH", "/api/admin/propiedades", "/1/imagenes/3/principal").actor(vendedor(51))).estado);
        assertEquals(403, ServletFalso.ejecutar(servlet,
                Peticion.de("DELETE", "/api/admin/propiedades", "/1/imagenes/3").actor(vendedor(51))).estado);
        assertTrue(admin.llamadas.isEmpty());
    }
}

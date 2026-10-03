package py.sn.inmobiliaria.service;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.dto.request.ImagenRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.CasaRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.OperacionRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.TerrenoRequest;
import py.sn.inmobiliaria.exception.FueraDeZonaException;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.prueba.DatosPrueba.Zona;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.service.PropiedadAdminService.PropiedadGuardada;
import py.sn.inmobiliaria.service.UbicacionService.UbicacionDetectada;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de los services contra la base real (solo si DB_URL está definida).
 *
 * <p>Los services confirman sus transacciones, así que la prueba LIMPIA lo que crea: al terminar
 * cada prueba borra las propiedades con id mayor al último que existía antes de empezar.
 * No espera cantidades ni códigos fijos: los calcula a partir del estado de la base, así funciona
 * aunque se hayan creado o editado propiedades desde el panel.</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ServiciosIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static Zona zona;

    private static PropiedadAdminService admin;
    private static PropiedadConsultaService consulta;
    private static CatalogoService catalogos;
    private static UbicacionService ubicacion;

    private long ultimoIdAntes;
    private long totalAntes;

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        TxRunner tx = new TxRunner(jpa);
        JdbcUbicacionDao ubicacionDao = new JdbcUbicacionDao(dataSource);
        JpaPropiedadDao propiedadDao = new JpaPropiedadDao();
        JpaCatalogoDao catalogoDao = new JpaCatalogoDao();

        ubicacion = new UbicacionService(ubicacionDao);
        admin = new PropiedadAdminService(tx, propiedadDao, catalogoDao, new JpaImagenDao(), ubicacionDao);
        consulta = new PropiedadConsultaService(tx, propiedadDao);
        catalogos = new CatalogoService(tx, catalogoDao, ubicacion);
        zona = DatosPrueba.zona(dataSource);
    }

    @BeforeEach
    void recordarEstado() {
        ultimoIdAntes = DatosPrueba.numero(dataSource, "SELECT COALESCE(MAX(id_propiedad), 0) FROM propiedad");
        totalAntes = DatosPrueba.numero(dataSource, "SELECT count(*) FROM propiedad");
    }

    @AfterEach
    void limpiarYVerificar() {
        DatosPrueba.numero(dataSource,
                "WITH borradas AS (DELETE FROM propiedad WHERE id_propiedad > ? RETURNING 1) SELECT count(*) FROM borradas",
                ultimoIdAntes); // casa, terreno, imágenes y operaciones se borran en cascada
        assertEquals(totalAntes, DatosPrueba.numero(dataSource, "SELECT count(*) FROM propiedad"),
                "La prueba no debe dejar propiedades en la base");
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

    // ---- Datos de prueba ----

    private static PropiedadRequest casa(List<OperacionRequest> ops) {
        return new PropiedadRequest("Casa de prueba", "Descripción", "Calle 1",
                zona.latitud(), zona.longitud(), zona.idBarrio(), "Casa", null,
                new CasaRequest(2, 1, true, true, true, new BigDecimal("95.5"), null), null, ops);
    }

    private static PropiedadRequest comoTerreno(PropiedadRequest r, List<OperacionRequest> ops) {
        return new PropiedadRequest(r.titulo(), r.descripcion(), r.direccion(), r.latitud(), r.longitud(),
                r.idBarrio(), "Terreno", r.estado(), null,
                new TerrenoRequest(new BigDecimal("360"), new BigDecimal("12"), new BigDecimal("30"), true), ops);
    }

    private static OperacionRequest op(String operacion, String precio, String moneda) {
        return new OperacionRequest(operacion, new BigDecimal(precio), moneda);
    }

    /** El código que debería recibir la próxima propiedad con ese prefijo. */
    private static String siguienteCodigo(String prefijo) {
        long ultimo = DatosPrueba.numero(dataSource,
                "SELECT COALESCE(MAX(CAST(substring(codigo FROM '[0-9]+$') AS integer)), 0) FROM propiedad WHERE codigo LIKE ?",
                prefijo + "-%");
        return ReglasNegocio.formatearCodigo(prefijo, (int) ultimo + 1);
    }

    private static long idPrincipal(long idPropiedad) {
        return admin.obtener(idPropiedad, p -> p.getImagenes().stream()
                .filter(Imagen::isEsPrincipal).map(Imagen::getId).findFirst().orElse(-1L));
    }

    // ---- Propiedades ----

    @Test
    void creaUnaCasaConVentaYAlquilerYApareceEnLaBusqueda() {
        String esperado = siguienteCodigo("prop");

        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "120000", "USD"), op("Alquiler", "4000000", "PYG"))));

        assertEquals(esperado, g.codigo());
        List<String> alquileresEnPyg = consulta.buscar(
                new BusquedaRequest(zona.idCiudad(), null, "Vivienda", "Alquiler", new BigDecimal("5000000"), "PYG"),
                Propiedad::getCodigo);
        assertTrue(alquileresEnPyg.contains(g.codigo()));
        assertEquals(2, (int) consulta.obtenerPorCodigo(g.codigo(), p -> p.getOperaciones().size()));
    }

    @Test
    void elCodigoDeTerrenoUsaSuPropioPrefijo() {
        String esperado = siguienteCodigo("terr");

        PropiedadGuardada g = admin.crear(comoTerreno(casa(null), List.of(op("Venta", "30000", "USD"))));

        assertEquals(esperado, g.codigo());
    }

    @Test
    void rechazaUnaUbicacionFueraDelBarrio() {
        PropiedadRequest base = casa(List.of(op("Venta", "1", "USD")));
        PropiedadRequest lejos = new PropiedadRequest(base.titulo(), null, null,
                DatosPrueba.LATITUD_FUERA, DatosPrueba.LONGITUD_FUERA, base.idBarrio(), "Casa", null,
                base.casa(), null, base.operaciones());

        ValidacionException e = assertThrows(ValidacionException.class, () -> admin.crear(lejos));

        assertTrue(e.campos().get("latitud").contains("no está dentro del barrio"));
    }

    @Test
    void cambiaDeCasaConAlquilerATerrenoSinCambiarElCodigo() {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "120000", "USD"), op("Alquiler", "4000000", "PYG"))));

        admin.actualizar(g.id(), comoTerreno(casa(null), List.of(op("Venta", "90000", "USD"))));

        String resumen = admin.obtener(g.id(), p -> p.getTipo().getNombre() + "|" + (p.getCasa() == null)
                + "|" + p.getTerreno().getSuperficie().intValue() + "|" + p.getOperaciones().size()
                + "|" + p.getOperaciones().get(0).getPrecio().intValue() + "|" + p.getCodigo());
        assertEquals("Terreno|true|360|1|90000|" + g.codigo(), resumen);
    }

    @Test
    void elBorradoEsLogico() {
        long disponiblesAntes = consulta.buscar(BusquedaRequest.vacia(), Propiedad::getCodigo).size();
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "1000", "USD"))));
        assertEquals(disponiblesAntes + 1, consulta.buscar(BusquedaRequest.vacia(), Propiedad::getCodigo).size());

        admin.eliminar(g.id());

        assertThrows(NoEncontradoException.class, () -> consulta.obtenerPorCodigo(g.codigo(), Propiedad::getCodigo));
        assertEquals("Inactivo", admin.obtener(g.id(), p -> p.getEstado().getNombre())); // sigue en la base
        assertEquals(disponiblesAntes, consulta.buscar(BusquedaRequest.vacia(), Propiedad::getCodigo).size());
    }

    // ---- Imágenes ----

    @Test
    void gestionaImagenesYSuPrincipal() {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "1000", "USD"))));

        long primera = admin.agregarImagen(g.id(), new ImagenRequest("/uploads/" + g.codigo() + "/a.jpg", "Frente"));
        long segunda = admin.agregarImagen(g.id(), new ImagenRequest("/uploads/" + g.codigo() + "/b.jpg", null));
        assertEquals(primera, idPrincipal(g.id()));       // la primera queda como principal

        admin.marcarPrincipal(g.id(), segunda);
        assertEquals(segunda, idPrincipal(g.id()));

        String url = admin.eliminarImagen(g.id(), segunda); // se borra la principal...
        assertEquals("/uploads/" + g.codigo() + "/b.jpg", url);
        assertEquals(primera, idPrincipal(g.id()));       // ...y la otra pasa a serlo
    }

    @Test
    void validaQueLaImagenPertenezcaALaPropiedad() {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "1000", "USD"))));

        assertThrows(NoEncontradoException.class, () -> admin.marcarPrincipal(g.id(), Long.MAX_VALUE));
        assertThrows(ValidacionException.class, () -> admin.agregarImagen(g.id(), new ImagenRequest("ftp://x", null)));
    }

    // ---- Búsqueda, catálogos y ubicación ----

    @Test
    void laBusquedaRechazaTerrenoEnAlquiler() {
        assertThrows(ValidacionException.class, () -> consulta.buscar(
                new BusquedaRequest(null, null, "Terreno", "Alquiler", null, null), Propiedad::getCodigo));
    }

    @Test
    void catalogosParaElFormulario() {
        CatalogoService.Catalogos c = catalogos.obtener();

        assertEquals(List.of("Casa", "Terreno"), c.tipos());
        assertEquals(5, c.estados().size());
        assertEquals(List.of("PYG", "USD"), c.monedas());
        assertEquals(List.of("Venta"), c.operacionesPorTipo().get("Terreno"));
        assertEquals(DatosPrueba.numero(dataSource, "SELECT count(*) FROM barrio"), c.ubicaciones().size());
    }

    @Test
    void propiedadInexistente() {
        assertThrows(NoEncontradoException.class, () -> admin.obtener(Long.MAX_VALUE, Propiedad::getCodigo));
        assertThrows(NoEncontradoException.class, () -> admin.eliminar(Long.MAX_VALUE));
    }

    @Test
    void laUbicacionDetectadaPermiteGuardarLaPropiedad() {
        // Flujo del panel: clic en el mapa → detección → guardar con el barrio detectado
        UbicacionDetectada u = ubicacion.localizar(zona.latitud(), zona.longitud());
        assertEquals(zona.idBarrio(), u.ubicacion().idBarrio());

        PropiedadRequest base = casa(List.of(op("Venta", "1000", "USD")));
        PropiedadRequest conDetectada = new PropiedadRequest(base.titulo(), null, null, u.latitud(), u.longitud(),
                u.ubicacion().idBarrio(), "Casa", null, base.casa(), null, base.operaciones());

        assertTrue(admin.crear(conDetectada).id() > 0);
    }

    @Test
    void unPuntoFueraDeZonaSeInformaConClaridad() {
        FueraDeZonaException e = assertThrows(FueraDeZonaException.class,
                () -> ubicacion.localizar(DatosPrueba.LATITUD_FUERA, DatosPrueba.LONGITUD_FUERA));

        assertEquals("FUERA_DE_ZONA", e.codigo());
        assertFalse(e.getMessage().isBlank());
    }
}

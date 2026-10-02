package py.sn.inmobiliaria.service;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.service.PropiedadAdminService.PropiedadGuardada;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de los services contra la base real (solo si DB_URL está definida).
 *
 * <p>Los services confirman sus transacciones, así que esta prueba LIMPIA lo que crea:
 * al terminar cada prueba borra las propiedades con id mayor al último de la carga
 * inicial. Las 28 propiedades originales no se modifican.</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ServiciosIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static long ultimoIdOriginal;

    private static PropiedadAdminService admin;
    private static PropiedadConsultaService consulta;
    private static CatalogoService catalogos;

    @BeforeAll
    static void iniciar() throws Exception {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        TxRunner tx = new TxRunner(jpa);
        JdbcUbicacionDao ubicacionDao = new JdbcUbicacionDao(dataSource);
        JpaPropiedadDao propiedadDao = new JpaPropiedadDao();
        JpaCatalogoDao catalogoDao = new JpaCatalogoDao();

        admin = new PropiedadAdminService(tx, propiedadDao, catalogoDao, new JpaImagenDao(), ubicacionDao);
        consulta = new PropiedadConsultaService(tx, propiedadDao);
        catalogos = new CatalogoService(tx, catalogoDao, new UbicacionService(ubicacionDao));
        ultimoIdOriginal = unNumero("SELECT COALESCE(MAX(id_propiedad), 0) FROM propiedad");
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM propiedad WHERE id_propiedad > ?")) {
            ps.setLong(1, ultimoIdOriginal); // casa, terreno, imágenes y operaciones se borran en cascada
            ps.executeUpdate();
        }
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

    // ---- Datos de prueba: un punto real dentro de AREA 1 (el de prop-001) ----

    private static PropiedadRequest casa(List<OperacionRequest> ops) throws Exception {
        return new PropiedadRequest("Casa de prueba", "Descripción", "Calle 1",
                coordenada("latitud"), coordenada("longitud"), 10011013, "Casa", null,
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

    private static BigDecimal coordenada(String columna) throws Exception {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT " + columna + " FROM propiedad WHERE codigo = 'prop-001'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }

    private static long unNumero(String sql) throws Exception {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // ---- Pruebas ----

    @Test
    void creaUnaCasaConVentaYAlquilerYApareceEnLaBusqueda() throws Exception {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "120000", "USD"), op("Alquiler", "4000000", "PYG"))));

        assertEquals("prop-017", g.codigo()); // siguiente a prop-016

        List<String> alquileresEnPyg = consulta.buscar(
                new BusquedaRequest(null, null, "Vivienda", "Alquiler", new BigDecimal("5000000"), "PYG"),
                Propiedad::getCodigo);
        assertEquals(List.of("prop-017"), alquileresEnPyg);

        int operaciones = consulta.obtenerPorCodigo("prop-017", p -> p.getOperaciones().size());
        assertEquals(2, operaciones);
    }

    @Test
    void elCodigoDeTerrenoUsaSuPropioPrefijo() throws Exception {
        PropiedadGuardada g = admin.crear(comoTerreno(casa(null), List.of(op("Venta", "30000", "USD"))));

        assertEquals("terr-013", g.codigo());
    }

    @Test
    void rechazaUnaUbicacionFueraDelBarrio() throws Exception {
        PropiedadRequest base = casa(List.of(op("Venta", "1", "USD")));
        PropiedadRequest lejos = new PropiedadRequest(base.titulo(), null, null,
                new BigDecimal("-25.300000"), new BigDecimal("-54.500000"), base.idBarrio(), "Casa", null,
                base.casa(), null, base.operaciones());

        ValidacionException e = assertThrows(ValidacionException.class, () -> admin.crear(lejos));

        assertTrue(e.campos().get("latitud").contains("no está dentro del barrio"));
    }

    @Test
    void cambiaDeCasaConAlquilerATerreno() throws Exception {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "120000", "USD"), op("Alquiler", "4000000", "PYG"))));

        admin.actualizar(g.id(), comoTerreno(casa(null), List.of(op("Venta", "90000", "USD"))));

        String resumen = admin.obtener(g.id(), p -> p.getTipo().getNombre() + "|" + (p.getCasa() == null)
                + "|" + p.getTerreno().getSuperficie().intValue() + "|" + p.getOperaciones().size()
                + "|" + p.getOperaciones().get(0).getPrecio().intValue() + "|" + p.getCodigo());
        assertEquals("Terreno|true|360|1|90000|prop-017", resumen); // el código no cambia
    }

    @Test
    void elBorradoEsLogico() throws Exception {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "1000", "USD"))));

        admin.eliminar(g.id());

        assertThrows(NoEncontradoException.class, () -> consulta.obtenerPorCodigo(g.codigo(), Propiedad::getCodigo));
        assertEquals("Inactivo", admin.obtener(g.id(), p -> p.getEstado().getNombre())); // sigue en la base
        assertEquals(28, consulta.buscar(BusquedaRequest.vacia(), Propiedad::getCodigo).size());
    }

    @Test
    void gestionaImagenesYSuPrincipal() throws Exception {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "1000", "USD"))));

        long primera = admin.agregarImagen(g.id(), new ImagenRequest("/uploads/prop-017/a.jpg", "Frente"));
        long segunda = admin.agregarImagen(g.id(), new ImagenRequest("/uploads/prop-017/b.jpg", null));
        assertEquals(primera, idPrincipal(g.id()));       // la primera queda como principal

        admin.marcarPrincipal(g.id(), segunda);
        assertEquals(segunda, idPrincipal(g.id()));

        admin.eliminarImagen(g.id(), segunda);             // se borra la principal...
        assertEquals(primera, idPrincipal(g.id()));       // ...y la otra pasa a serlo
    }

    @Test
    void validaQueLaImagenPertenezcaALaPropiedad() throws Exception {
        PropiedadGuardada g = admin.crear(casa(List.of(op("Venta", "1000", "USD"))));

        assertThrows(NoEncontradoException.class, () -> admin.marcarPrincipal(g.id(), 999999L));
        assertThrows(ValidacionException.class, () -> admin.agregarImagen(g.id(), new ImagenRequest("ftp://x", null)));
    }

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
        assertEquals(12, c.ubicaciones().size());
    }

    @Test
    void propiedadInexistente() {
        assertThrows(NoEncontradoException.class, () -> admin.obtener(999999L, Propiedad::getCodigo));
        assertThrows(NoEncontradoException.class, () -> admin.eliminar(999999L));
    }

    private static long idPrincipal(long idPropiedad) {
        return admin.obtener(idPropiedad, p -> p.getImagenes().stream()
                .filter(Imagen::isEsPrincipal).map(Imagen::getId).findFirst().orElse(-1L));
    }

    @Test
    void sinCambiosEnLasPropiedadesOriginales() throws Exception {
        assertNull(null);
        assertFalse(unNumero("SELECT count(*) FROM propiedad WHERE id_propiedad <= " + ultimoIdOriginal) != 28);
    }
}

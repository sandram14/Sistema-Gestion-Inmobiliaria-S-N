package py.sn.inmobiliaria.repository;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.model.TipoPropiedad;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.repository.impl.JpaUsuarioDao;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de los DAOs JPA contra la base real.
 * Solo se ejecuta si está definida la variable DB_URL. Las escrituras se deshacen siempre.
 *
 * <p>Los valores esperados corresponden a los datos de database/seed/03_propiedades_demo.sql.</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PropiedadDaoIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static TxRunner tx;

    private final PropiedadDao propiedadDao = new JpaPropiedadDao();
    private final CatalogoDao catalogoDao = new JpaCatalogoDao();
    private final ImagenDao imagenDao = new JpaImagenDao();
    private final UsuarioDao usuarioDao = new JpaUsuarioDao();

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        tx = new TxRunner(jpa);
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

    private static void enTransaccionDescartada(Consumer<EntityManager> bloque) {
        EntityManager em = jpa.emf().createEntityManager();
        try {
            em.getTransaction().begin();
            bloque.accept(em);
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    private List<Propiedad> buscar(FiltroPropiedades filtro) {
        return tx.soloLectura(em -> propiedadDao.buscarDisponibles(em, filtro));
    }

    private static Integer idCiudad(String nombre) {
        return tx.soloLectura(em -> em.createQuery(
                        "select c.id from Ciudad c where c.nombre = :n", Integer.class)
                .setParameter("n", nombre).getSingleResult());
    }

    // ---- Búsqueda pública ----

    @Test
    void sinFiltrosDevuelveLas28DisponiblesOrdenadas() {
        List<Propiedad> todas = buscar(FiltroPropiedades.sinFiltros());

        assertEquals(28, todas.size());
        assertEquals("prop-001", todas.get(0).getCodigo());
    }

    @Test
    void filtraPorTipo() {
        List<Propiedad> terrenos = buscar(new FiltroPropiedades(null, null, TipoPropiedad.TERRENO, null, null, null));

        assertEquals(12, terrenos.size());
        assertTrue(terrenos.stream().allMatch(p -> p.getTipo().getNombre().equals(TipoPropiedad.TERRENO)));
    }

    @Test
    void filtraPorCiudadYBarrio() {
        assertEquals(12, buscar(new FiltroPropiedades(idCiudad("CIUDAD DEL ESTE"), null, null, null, null, null)).size());
        assertEquals(4, buscar(new FiltroPropiedades(idCiudad("HERNANDARIAS"), null, null, null, null, null)).size());
        assertEquals(3, buscar(new FiltroPropiedades(null, 10011013, null, null, null, null)).size());
    }

    @Test
    void filtraPorDepartamento() {
        Integer altoParana = tx.soloLectura(em -> em.createQuery(
                        "select d.id from Departamento d where d.codigo = '10'", Integer.class).getSingleResult());

        assertEquals(28, buscar(new FiltroPropiedades(null, null, null, null, null, null, altoParana)).size());
        assertEquals(0, buscar(new FiltroPropiedades(null, null, null, null, null, null, 999999)).size());
        // Departamento + ciudad: se combinan
        assertEquals(12, buscar(new FiltroPropiedades(idCiudad("CIUDAD DEL ESTE"), null, null, null, null, null,
                altoParana)).size());
    }

    @Test
    void filtraPorOperacion() {
        List<Propiedad> alquileres = buscar(new FiltroPropiedades(null, null, null, Operacion.ALQUILER, null, null));

        assertEquals(5, alquileres.size());
        assertTrue(alquileres.stream().allMatch(p -> p.getTipo().getNombre().equals(TipoPropiedad.CASA)));
    }

    @Test
    void terrenoEnAlquilerNoDevuelveResultados() {
        // El service rechazará esta combinación antes de llegar aquí; el DAO simplemente no encuentra nada
        assertEquals(0, buscar(new FiltroPropiedades(null, null, TipoPropiedad.TERRENO, Operacion.ALQUILER, null, null)).size());
    }

    @Test
    void filtraPorPresupuestoSoloEnLaMonedaElegida() {
        assertEquals(10, buscar(new FiltroPropiedades(null, null, null, Operacion.VENTA,
                new BigDecimal("50000"), Moneda.USD)).size());
        // Todos los precios cargados están en USD y no se convierte entre monedas
        assertEquals(0, buscar(new FiltroPropiedades(null, null, null, Operacion.VENTA,
                new BigDecimal("999999999"), Moneda.PYG)).size());
    }

    @Test
    void combinaVariosFiltros() {
        assertEquals(3, buscar(new FiltroPropiedades(idCiudad("CIUDAD DEL ESTE"), null, TipoPropiedad.CASA,
                Operacion.VENTA, new BigDecimal("100000"), Moneda.USD)).size());
    }

    @Test
    void leeLasRelacionesDentroDeLaTransaccion() {
        tx.soloLectura(em -> {
            Propiedad p = propiedadDao.buscarDisponiblePorCodigo(em, "prop-001").orElseThrow();
            assertEquals("CIUDAD DEL ESTE", p.getBarrio().getCiudad().getNombre());
            assertEquals(5, p.getImagenes().size());
            assertEquals(1, p.getOperaciones().size());
            return null;
        });
    }

    @Test
    void unaPropiedadInactivaNoApareceEnLaPartePublica() {
        enTransaccionDescartada(em -> {
            Propiedad p = propiedadDao.buscarDisponiblePorCodigo(em, "prop-003").orElseThrow();
            p.setEstado(catalogoDao.estadoPorNombre(em, EstadoPropiedad.INACTIVO).orElseThrow());
            em.flush();

            assertEquals(27, propiedadDao.buscarDisponibles(em, FiltroPropiedades.sinFiltros()).size());
            assertTrue(propiedadDao.buscarDisponiblePorCodigo(em, "prop-003").isEmpty());
            assertTrue(propiedadDao.buscarPorId(em, p.getId()).isPresent());   // el panel sí la ve
            assertEquals(28, propiedadDao.listarTodas(em).size());
        });
    }

    // ---- Panel administrativo ----

    @Test
    void codigosExistentesYSiguienteNumero() {
        tx.soloLectura(em -> {
            assertTrue(propiedadDao.existeCodigo(em, "prop-001"));
            assertFalse(propiedadDao.existeCodigo(em, "prop-999"));
            assertEquals(16, propiedadDao.ultimoNumeroDeCodigo(em, "prop"));
            assertEquals(12, propiedadDao.ultimoNumeroDeCodigo(em, "terr"));
            assertEquals(0, propiedadDao.ultimoNumeroDeCodigo(em, "xyz"));
            return null;
        });
    }

    @Test
    void cambiaLaImagenPrincipalSinViolarElIndiceUnico() {
        enTransaccionDescartada(em -> {
            Propiedad p = propiedadDao.buscarDisponiblePorCodigo(em, "prop-001").orElseThrow();
            Imagen segunda = p.getImagenes().get(1);

            assertEquals(1, imagenDao.desmarcarPrincipal(em, p.getId()));
            segunda.setEsPrincipal(true);
            em.flush();   // si hubiera dos principales, aquí fallaría el índice uq_imagen_principal

            Imagen principal = imagenDao.buscar(em, p.getId(), segunda.getId()).orElseThrow();
            assertTrue(principal.isEsPrincipal());
        });
    }

    @Test
    void catalogosYBarrios() {
        tx.soloLectura(em -> {
            assertEquals(2, catalogoDao.listarTipos(em).size());
            assertEquals(5, catalogoDao.listarEstados(em).size());
            assertEquals(2, catalogoDao.listarOperaciones(em).size());
            assertTrue(catalogoDao.operacionPorNombre(em, Operacion.VENTA).isPresent());
            assertTrue(catalogoDao.tipoPorNombre(em, "Departamento").isEmpty());
            assertEquals("AREA 1", catalogoDao.barrioPorId(em, 10011013).orElseThrow().getNombre());
            assertTrue(catalogoDao.barrioPorId(em, 1).isEmpty());
            return null;
        });
    }

    @Test
    void buscaUsuarioActivoSinDistinguirMayusculas() {
        enTransaccionDescartada(em -> {
            Rol admin = catalogoDao.rolPorNombre(em, "ADMIN").orElseThrow();
            // Hash BCrypt de ejemplo (60 caracteres); en esta prueba no se verifica la contraseña
            String hash = "$2a$12$" + "a".repeat(53);
            em.persist(new Usuario("Prueba", "Dao", "prueba.dao@test.com", hash, admin));
            em.flush();

            assertTrue(usuarioDao.buscarActivoPorEmail(em, "PRUEBA.DAO@test.com").isPresent());
            assertTrue(usuarioDao.buscarActivoPorEmail(em, "otro@test.com").isEmpty());
        });
    }
}

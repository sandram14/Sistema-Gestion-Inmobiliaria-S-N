package py.sn.inmobiliaria.model;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración: Hibernate contra la base real (Docker).
 *
 * <p>Solo se ejecuta si está definida la variable DB_URL; en una compilación normal
 * ({@code mvn package} sin variables) se omite. Las pruebas de escritura se deshacen
 * siempre (rollback): la base queda igual.</p>
 *
 * <pre>
 * $env:DB_URL = "jdbc:postgresql://localhost:5433/sn_inmobiliaria"
 * $env:DB_USER = "sn_app"; $env:DB_PASSWORD = "..."
 * mvn -f backend/pom.xml test
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ModeloJpaIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static TxRunner tx;

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

    /** Ejecuta el bloque en una transacción que SIEMPRE se deshace al final. */
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

    private static Propiedad buscar(EntityManager em, String codigo) {
        return em.createQuery("select p from Propiedad p where p.codigo = :c", Propiedad.class)
                .setParameter("c", codigo)
                .getSingleResult();
    }

    private static <T> T catalogo(EntityManager em, Class<T> clase, String nombre) {
        return em.createQuery("select x from " + clase.getSimpleName() + " x where x.nombre = :n", clase)
                .setParameter("n", nombre)
                .getSingleResult();
    }

    @Test
    void hibernateValidaElEsquemaAlIniciar() {
        // Crear el EntityManagerFactory ejecuta la validación (hbm2ddl=validate)
        assertTrue(jpa.emf().isOpen());
    }

    @Test
    void cargaTodasLasPropiedadesConSusSubtipos() {
        tx.soloLectura(em -> {
            assertEquals(28L, em.createQuery("select count(p) from Propiedad p", Long.class).getSingleResult());
            assertEquals(16L, em.createQuery("select count(c) from Casa c", Long.class).getSingleResult());
            assertEquals(12L, em.createQuery("select count(t) from Terreno t", Long.class).getSingleResult());
            assertEquals(12L, em.createQuery("select count(b) from Barrio b", Long.class).getSingleResult());
            return null;
        });
    }

    @Test
    void leeUnaCasaConTodasSusRelaciones() {
        tx.soloLectura(em -> {
            Propiedad p = buscar(em, "prop-001");

            assertEquals("Casa en Area 1", p.getTitulo());
            assertEquals(TipoPropiedad.CASA, p.getTipo().getNombre());
            assertEquals(EstadoPropiedad.DISPONIBLE, p.getEstado().getNombre());
            assertEquals(10011013, p.getBarrio().getId());
            assertEquals("AREA 1", p.getBarrio().getNombre());
            assertEquals("CIUDAD DEL ESTE", p.getBarrio().getCiudad().getNombre());
            assertEquals("10", p.getBarrio().getCiudad().getDepartamento().getCodigo());
            assertNotNull(p.getLatitud());
            assertNotNull(p.getCreadoEn());

            Casa casa = p.getCasa();
            assertNotNull(casa);
            assertNull(p.getTerreno());
            assertEquals((short) 3, casa.getHabitaciones());
            assertEquals((short) 2, casa.getBanos());
            assertEquals(0, new BigDecimal("182").compareTo(casa.getSuperficieConstruida()));

            assertEquals(1, p.getOperaciones().size());
            OperacionPropiedad venta = p.getOperaciones().get(0);
            assertEquals(Operacion.VENTA, venta.getOperacion().getNombre());
            assertEquals(0, new BigDecimal("45000").compareTo(venta.getPrecio()));
            assertEquals(Moneda.USD, venta.getMoneda());

            assertEquals(5, p.getImagenes().size());
            assertTrue(p.getImagenes().get(0).isEsPrincipal());
            assertEquals("/images/propiedades/prop-001/1.png", p.getImagenes().get(0).getUrl());
            assertFalse(p.getImagenes().get(1).isEsPrincipal());
            return null;
        });
    }

    @Test
    void leeUnTerreno() {
        tx.soloLectura(em -> {
            Propiedad p = buscar(em, "terr-001");

            assertEquals(TipoPropiedad.TERRENO, p.getTipo().getNombre());
            assertNull(p.getCasa());
            assertNotNull(p.getTerreno());
            assertEquals(0, new BigDecimal("750").compareTo(p.getTerreno().getSuperficie()));
            assertFalse(p.getTerreno().isTieneTitulo());
            assertEquals("HERNANDARIAS", p.getBarrio().getCiudad().getNombre());
            assertEquals(0, new BigDecimal("41000").compareTo(p.getOperaciones().get(0).getPrecio()));
            return null;
        });
    }

    @Test
    void agregaUnaOperacionAlquilerEnPygAUnaCasa() {
        enTransaccionDescartada(em -> {
            Propiedad p = buscar(em, "prop-001");
            p.agregarOperacion(new OperacionPropiedad(
                    catalogo(em, Operacion.class, Operacion.ALQUILER), new BigDecimal("4000000"), Moneda.PYG));
            em.flush();

            Long cantidad = em.createQuery(
                            "select count(op) from OperacionPropiedad op where op.propiedad.codigo = 'prop-001'", Long.class)
                    .getSingleResult();
            assertEquals(2L, cantidad);
        });
    }

    @Test
    void elTriggerRechazaUnTerrenoEnAlquiler() {
        enTransaccionDescartada(em -> {
            Propiedad terreno = buscar(em, "terr-001");
            terreno.agregarOperacion(new OperacionPropiedad(
                    catalogo(em, Operacion.class, Operacion.ALQUILER), new BigDecimal("1000000"), Moneda.PYG));

            assertThrows(PersistenceException.class, em::flush);
        });
    }

    @Test
    void creaUnaPropiedadCompletaEnCascada() {
        enTransaccionDescartada(em -> {
            Propiedad referencia = buscar(em, "prop-001"); // misma ubicación y barrio válidos

            Propiedad nueva = new Propiedad("prop-999", "Casa de prueba",
                    referencia.getLatitud(), referencia.getLongitud(),
                    catalogo(em, TipoPropiedad.class, TipoPropiedad.CASA),
                    catalogo(em, EstadoPropiedad.class, EstadoPropiedad.DISPONIBLE),
                    referencia.getBarrio());
            Casa casa = new Casa((short) 2, (short) 1, new BigDecimal("95.50"));
            casa.setCocina(true);
            nueva.asignarCasa(casa);
            nueva.agregarImagen(new Imagen("/uploads/prop-999/a.jpg", true, (short) 0));
            nueva.agregarOperacion(new OperacionPropiedad(
                    catalogo(em, Operacion.class, Operacion.VENTA), new BigDecimal("80000"), Moneda.USD));

            em.persist(nueva);
            em.flush();

            assertNotNull(nueva.getId());
            assertNotNull(casa.getId());
            assertEquals(nueva.getId(), nueva.getOperaciones().get(0).getId().getIdPropiedad());
        });
    }

    @Test
    void guardaUnaConsultaDelAsistente() {
        enTransaccionDescartada(em -> {
            Consulta c = new Consulta(UUID.randomUUID(), "¿Esta casa tiene cochera?", buscar(em, "prop-015"));
            c.setRespuesta("Según los datos disponibles, no se indica cochera.");
            em.persist(c);
            em.flush();

            assertNotNull(c.getId());
        });
    }
}

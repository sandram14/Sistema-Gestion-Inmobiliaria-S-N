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
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.prueba.DatosPrueba.Zona;

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
 * <p>No depende de las propiedades de demostración: cada prueba crea sus propias propiedades
 * dentro de una transacción que se deshace siempre (rollback), y relee los datos desde la base
 * para comprobar el mapeo de ida y vuelta.</p>
 *
 * <p>Solo se ejecuta si está definida la variable DB_URL (ver README del backend).</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ModeloJpaIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static TxRunner tx;
    private static Zona zona;

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        tx = new TxRunner(jpa);
        zona = DatosPrueba.zona(dataSource);
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

    /** Guarda, vacía la memoria de Hibernate y vuelve a leer desde la base (mapeo de ida y vuelta). */
    private static Propiedad guardarYReleer(EntityManager em, Propiedad p) {
        em.persist(p);
        em.flush();
        em.clear();
        return em.createQuery("select p from Propiedad p where p.codigo = :c", Propiedad.class)
                .setParameter("c", p.getCodigo())
                .getSingleResult();
    }

    @Test
    void hibernateValidaElEsquemaAlIniciar() {
        // Crear el EntityManagerFactory ejecuta la validación (hbm2ddl=validate)
        assertTrue(jpa.emf().isOpen());
    }

    @Test
    void cadaPropiedadTieneExactamenteUnSubtipo() {
        tx.soloLectura(em -> {
            long propiedades = em.createQuery("select count(p) from Propiedad p", Long.class).getSingleResult();
            long casas = em.createQuery("select count(c) from Casa c", Long.class).getSingleResult();
            long terrenos = em.createQuery("select count(t) from Terreno t", Long.class).getSingleResult();
            assertEquals(propiedades, casas + terrenos);

            long barriosJpa = em.createQuery("select count(b) from Barrio b", Long.class).getSingleResult();
            assertEquals(DatosPrueba.numero(dataSource, "SELECT count(*) FROM barrio"), barriosJpa);
            return null;
        });
    }

    @Test
    void guardaYLeeUnaCasaConTodasSusRelaciones() {
        enTransaccionDescartada(em -> {
            Propiedad nueva = DatosPrueba.casa(em, "test-901", zona);
            nueva.setDireccion("Calle de prueba 123");
            nueva.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "45000", Moneda.USD));
            for (short i = 0; i < 3; i++) {
                nueva.agregarImagen(new Imagen("/uploads/test-901/" + i + ".jpg", i == 0, i));
            }

            Propiedad p = guardarYReleer(em, nueva);

            assertEquals(TipoPropiedad.CASA, p.getTipo().getNombre());
            assertEquals(EstadoPropiedad.DISPONIBLE, p.getEstado().getNombre());
            assertEquals(zona.idBarrio(), p.getBarrio().getId());
            assertEquals(zona.ciudad(), p.getBarrio().getCiudad().getNombre());
            assertEquals(zona.departamento(), p.getBarrio().getCiudad().getDepartamento().getNombre());
            assertEquals(0, zona.latitud().compareTo(p.getLatitud()));
            assertNotNull(p.getCreadoEn()); // lo completa PostgreSQL

            assertNotNull(p.getCasa());
            assertNull(p.getTerreno());
            assertEquals((short) 3, p.getCasa().getHabitaciones());
            assertTrue(p.getCasa().isCocina());
            assertEquals(0, new BigDecimal("120").compareTo(p.getCasa().getSuperficieConstruida()));

            assertEquals(1, p.getOperaciones().size());
            assertEquals(Operacion.VENTA, p.getOperaciones().get(0).getOperacion().getNombre());
            assertEquals(Moneda.USD, p.getOperaciones().get(0).getMoneda());

            assertEquals(3, p.getImagenes().size());
            assertTrue(p.getImagenes().get(0).isEsPrincipal());   // la principal primero
            assertFalse(p.getImagenes().get(1).isEsPrincipal());
        });
    }

    @Test
    void guardaYLeeUnTerreno() {
        enTransaccionDescartada(em -> {
            Propiedad nuevo = DatosPrueba.terreno(em, "test-902", zona);
            nuevo.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "41000", Moneda.USD));

            Propiedad p = guardarYReleer(em, nuevo);

            assertEquals(TipoPropiedad.TERRENO, p.getTipo().getNombre());
            assertNull(p.getCasa());
            assertEquals(0, new BigDecimal("500").compareTo(p.getTerreno().getSuperficie()));
            assertFalse(p.getTerreno().isTieneTitulo());
        });
    }

    @Test
    void unaCasaPuedeTenerVentaEnUsdYAlquilerEnPyg() {
        enTransaccionDescartada(em -> {
            Propiedad nueva = DatosPrueba.casa(em, "test-903", zona);
            nueva.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "120000", Moneda.USD));
            nueva.agregarOperacion(DatosPrueba.operacion(em, Operacion.ALQUILER, "4000000", Moneda.PYG));

            Propiedad p = guardarYReleer(em, nueva);

            assertEquals(2, p.getOperaciones().size());
            assertEquals(p.getId(), p.getOperaciones().get(0).getId().getIdPropiedad()); // clave compuesta
        });
    }

    @Test
    void elTriggerRechazaUnTerrenoEnAlquiler() {
        enTransaccionDescartada(em -> {
            Propiedad terreno = DatosPrueba.terreno(em, "test-904", zona);
            terreno.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "30000", Moneda.USD));
            em.persist(terreno);
            em.flush();

            terreno.agregarOperacion(DatosPrueba.operacion(em, Operacion.ALQUILER, "1000000", Moneda.PYG));
            assertThrows(PersistenceException.class, em::flush);
        });
    }

    @Test
    void elTriggerRechazaUnaUbicacionFueraDelBarrio() {
        enTransaccionDescartada(em -> {
            Propiedad p = DatosPrueba.casa(em, "test-905", zona);
            p.setLatitud(DatosPrueba.LATITUD_FUERA);
            p.setLongitud(DatosPrueba.LONGITUD_FUERA);
            p.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "1000", Moneda.USD));

            // Con ids IDENTITY, Hibernate ejecuta el INSERT de la propiedad en el mismo persist()
            // (necesita el id generado), así que el trigger puede rechazarla ahí o en el flush.
            assertThrows(PersistenceException.class, () -> {
                em.persist(p);
                em.flush();
            });
        });
    }

    @Test
    void guardaUnaConsultaDelAsistente() {
        enTransaccionDescartada(em -> {
            Propiedad p = DatosPrueba.casa(em, "test-906", zona);
            p.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "1000", Moneda.USD));
            em.persist(p);

            Consulta c = new Consulta(UUID.randomUUID(), "¿Esta casa tiene cochera?", p);
            c.setRespuesta("Según los datos disponibles, no se indica cochera.");
            em.persist(c);
            em.flush();

            assertNotNull(c.getId());
        });
    }
}

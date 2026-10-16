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
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.OperacionPropiedad;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.model.TipoPropiedad;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.prueba.DatosPrueba.Zona;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.repository.impl.JpaUsuarioDao;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración de los DAOs JPA contra la base real (solo si DB_URL está definida).
 *
 * <p>Independiente de los datos de demostración: cada prueba crea sus propias propiedades
 * dentro de una transacción que se deshace al final, y verifica que (1) aparezcan donde deben,
 * (2) no aparezcan donde no deben y (3) TODO resultado cumpla el filtro, sin importar cuántas
 * otras propiedades haya en la base.</p>
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PropiedadDaoIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static Zona zona;

    private final PropiedadDao propiedadDao = new JpaPropiedadDao();
    private final CatalogoDao catalogoDao = new JpaCatalogoDao();
    private final ImagenDao imagenDao = new JpaImagenDao();
    private final UsuarioDao usuarioDao = new JpaUsuarioDao();

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
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

    /** Casa de prueba con Venta Gs. 850.000.000 y Alquiler Gs. 4.000.000. */
    private static Propiedad casaConDosOperaciones(EntityManager em, String codigo) {
        Propiedad p = DatosPrueba.casa(em, codigo, zona);
        p.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "850000000", Moneda.PYG));
        p.agregarOperacion(DatosPrueba.operacion(em, Operacion.ALQUILER, "4000000", Moneda.PYG));
        em.persist(p);
        return p;
    }

    /** Terreno de prueba con Venta Gs. 300.000.000. */
    private static Propiedad terrenoEnVenta(EntityManager em, String codigo) {
        Propiedad p = DatosPrueba.terreno(em, codigo, zona);
        p.agregarOperacion(DatosPrueba.operacion(em, Operacion.VENTA, "300000000", Moneda.PYG));
        em.persist(p);
        return p;
    }

    private static List<String> codigos(List<Propiedad> propiedades) {
        return propiedades.stream().map(Propiedad::getCodigo).toList();
    }

    private static boolean tieneOperacion(Propiedad p, Predicate<OperacionPropiedad> condicion) {
        return p.getOperaciones().stream().anyMatch(condicion);
    }

    // ---- Búsqueda pública ----

    @Test
    void sinFiltrosIncluyeLaNuevaYDevuelveOrdenadoPorCodigo() {
        enTransaccionDescartada(em -> {
            long antes = propiedadDao.contarDisponibles(em);
            casaConDosOperaciones(em, "test-911");
            em.flush();

            List<Propiedad> todas = propiedadDao.buscarDisponibles(em, FiltroPropiedades.sinFiltros());

            assertEquals(antes + 1, todas.size());
            assertEquals(antes + 1, propiedadDao.contarDisponibles(em));
            assertTrue(codigos(todas).contains("test-911"));
            List<String> ordenados = codigos(todas).stream().sorted().toList();
            assertEquals(ordenados, codigos(todas));
        });
    }

    @Test
    void filtraPorTipo() {
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-912");
            terrenoEnVenta(em, "test-913");
            em.flush();

            List<Propiedad> terrenos = propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(null, null, TipoPropiedad.TERRENO, null, null, null));

            assertTrue(codigos(terrenos).contains("test-913"));
            assertFalse(codigos(terrenos).contains("test-912"));
            assertTrue(terrenos.stream().allMatch(p -> p.getTipo().getNombre().equals(TipoPropiedad.TERRENO)));
        });
    }

    @Test
    void filtraPorDepartamentoCiudadYBarrio() {
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-914");
            em.flush();

            List<Propiedad> enDepartamento = propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(null, null, null, null, null, null, zona.idDepartamento()));
            List<Propiedad> enCiudad = propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(zona.idCiudad(), null, null, null, null, null, zona.idDepartamento()));
            List<Propiedad> enBarrio = propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(null, zona.idBarrio(), null, null, null, null));

            assertTrue(codigos(enDepartamento).contains("test-914"));
            assertTrue(codigos(enCiudad).contains("test-914"));
            assertTrue(codigos(enBarrio).contains("test-914"));
            assertTrue(enDepartamento.stream().allMatch(p ->
                    p.getBarrio().getCiudad().getDepartamento().getId() == zona.idDepartamento()));
            assertTrue(enCiudad.stream().allMatch(p -> p.getBarrio().getCiudad().getId() == zona.idCiudad()));
            assertTrue(enBarrio.stream().allMatch(p -> p.getBarrio().getId() == zona.idBarrio()));
            assertEquals(0, propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(null, null, null, null, null, null, -1)).size());
        });
    }

    @Test
    void filtrarPorOtraCiudadExcluyeLaPropiedad() {
        var otra = DatosPrueba.zonaDeOtraCiudad(dataSource, zona.idCiudad());
        if (otra.isEmpty()) {
            return; // con una sola ciudad registrada no hay con qué comparar
        }
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-915");
            em.flush();

            List<Propiedad> enOtraCiudad = propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(otra.get().idCiudad(), null, null, null, null, null));

            assertFalse(codigos(enOtraCiudad).contains("test-915"));
        });
    }

    @Test
    void filtraPorOperacion() {
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-916");
            terrenoEnVenta(em, "test-917");
            em.flush();

            List<Propiedad> alquileres = propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(null, null, null, Operacion.ALQUILER, null, null));

            assertTrue(codigos(alquileres).contains("test-916"));
            assertFalse(codigos(alquileres).contains("test-917"));
            assertTrue(alquileres.stream().allMatch(p ->
                    tieneOperacion(p, op -> op.getOperacion().getNombre().equals(Operacion.ALQUILER))));
        });
    }

    @Test
    void terrenoEnAlquilerNuncaDevuelveResultados() {
        enTransaccionDescartada(em -> {
            terrenoEnVenta(em, "test-918");
            em.flush();

            // El service rechaza esta combinación antes; el DAO, por la regla de negocio, no encuentra nada
            assertEquals(0, propiedadDao.buscarDisponibles(em,
                    new FiltroPropiedades(null, null, TipoPropiedad.TERRENO, Operacion.ALQUILER, null, null)).size());
        });
    }

    @Test
    void elPresupuestoSeComparaContraElPrecioDeLaOperacion() {
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-919"); // Venta Gs. 850.000.000, Alquiler Gs. 4.000.000
            em.flush();

            assertTrue(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null,
                    Operacion.VENTA, new BigDecimal("900000000"), Moneda.PYG))).contains("test-919"));
            assertFalse(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null,
                    Operacion.VENTA, new BigDecimal("800000000"), Moneda.PYG))).contains("test-919"));
            assertTrue(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null,
                    Operacion.ALQUILER, new BigDecimal("5000000"), Moneda.PYG))).contains("test-919"));
            // Con operación Alquiler se compara el alquiler, no la venta
            assertFalse(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null,
                    Operacion.ALQUILER, new BigDecimal("3000000"), Moneda.PYG))).contains("test-919"));

            List<Propiedad> hasta900Millones = propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null,
                    Operacion.VENTA, new BigDecimal("900000000"), Moneda.PYG));
            assertTrue(hasta900Millones.stream().allMatch(p -> tieneOperacion(p, op ->
                    op.getOperacion().getNombre().equals(Operacion.VENTA)
                            && op.getPrecio().compareTo(new BigDecimal("900000000")) <= 0)));
        });
    }

    @Test
    void desdeYHastaSinOperacionBuscanEnCualquieraDeSusPrecios() {
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-940"); // Venta Gs. 850.000.000, Alquiler Gs. 4.000.000
            em.flush();

            // Entre 3 y 5 millones: entra por el alquiler
            assertTrue(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null, null,
                    new BigDecimal("5000000"), Moneda.PYG, null, new BigDecimal("3000000")))).contains("test-940"));
            // Solo "Desde" 1.000 millones: ningún precio llega
            assertFalse(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null, null,
                    null, Moneda.PYG, null, new BigDecimal("1000000000")))).contains("test-940"));
            // Solo "Desde" 500 millones: entra por la venta
            assertTrue(codigos(propiedadDao.buscarDisponibles(em, new FiltroPropiedades(null, null, null, null,
                    null, Moneda.PYG, null, new BigDecimal("500000000")))).contains("test-940"));
        });
    }

    @Test
    void combinaVariosFiltros() {
        enTransaccionDescartada(em -> {
            casaConDosOperaciones(em, "test-920");
            em.flush();

            List<Propiedad> r = propiedadDao.buscarDisponibles(em, new FiltroPropiedades(zona.idCiudad(), null,
                    TipoPropiedad.CASA, Operacion.VENTA, new BigDecimal("900000000"), Moneda.PYG, zona.idDepartamento()));

            assertTrue(codigos(r).contains("test-920"));
            assertTrue(r.stream().allMatch(p -> p.getTipo().getNombre().equals(TipoPropiedad.CASA)
                    && p.getBarrio().getCiudad().getId() == zona.idCiudad()));
        });
    }

    @Test
    void leeLasRelacionesDentroDeLaTransaccion() {
        enTransaccionDescartada(em -> {
            Propiedad nueva = casaConDosOperaciones(em, "test-921");
            nueva.agregarImagen(new Imagen("/uploads/test-921/a.jpg", true, (short) 0));
            em.flush();
            em.clear();

            Propiedad p = propiedadDao.buscarDisponiblePorCodigo(em, "test-921").orElseThrow();
            assertEquals(zona.ciudad(), p.getBarrio().getCiudad().getNombre());
            assertEquals(1, p.getImagenes().size());
            assertEquals(2, p.getOperaciones().size());
        });
    }

    @Test
    void unaPropiedadInactivaNoApareceEnLaPartePublica() {
        enTransaccionDescartada(em -> {
            Propiedad p = casaConDosOperaciones(em, "test-922");
            em.flush();
            long disponibles = propiedadDao.contarDisponibles(em);
            long todas = propiedadDao.listarTodas(em).size();

            p.setEstado(catalogoDao.estadoPorNombre(em, EstadoPropiedad.INACTIVO).orElseThrow());
            em.flush();

            assertEquals(disponibles - 1, propiedadDao.contarDisponibles(em));
            assertTrue(propiedadDao.buscarDisponiblePorCodigo(em, "test-922").isEmpty());
            assertTrue(propiedadDao.buscarPorId(em, p.getId()).isPresent()); // el panel sí la ve
            assertEquals(todas, propiedadDao.listarTodas(em).size());
        });
    }

    // ---- Panel administrativo ----

    @Test
    void codigosExistentesYSiguienteNumero() {
        enTransaccionDescartada(em -> {
            int antes = propiedadDao.ultimoNumeroDeCodigo(em, "test");
            casaConDosOperaciones(em, "test-9950");
            em.flush();

            assertTrue(propiedadDao.existeCodigo(em, "test-9950"));
            assertFalse(propiedadDao.existeCodigo(em, "test-9951"));
            assertEquals(Math.max(antes, 9950), propiedadDao.ultimoNumeroDeCodigo(em, "test"));
            assertEquals(0, propiedadDao.ultimoNumeroDeCodigo(em, "prefijoinexistente"));
        });
    }

    @Test
    void cambiaLaImagenPrincipalSinViolarElIndiceUnico() {
        enTransaccionDescartada(em -> {
            Propiedad p = casaConDosOperaciones(em, "test-923");
            p.agregarImagen(new Imagen("/uploads/test-923/a.jpg", true, (short) 0));
            p.agregarImagen(new Imagen("/uploads/test-923/b.jpg", false, (short) 1));
            em.flush();
            Imagen segunda = p.getImagenes().get(1);

            assertEquals(1, imagenDao.desmarcarPrincipal(em, p.getId()));
            segunda.setEsPrincipal(true);
            em.flush(); // si hubiera dos principales, aquí fallaría el índice uq_imagen_principal

            assertTrue(imagenDao.buscar(em, p.getId(), segunda.getId()).orElseThrow().isEsPrincipal());
        });
    }

    @Test
    void catalogosYBarrios() {
        enTransaccionDescartada(em -> {
            // Valores definidos por las reglas del negocio (no son datos de demostración)
            assertEquals(List.of(TipoPropiedad.CASA, TipoPropiedad.TERRENO),
                    catalogoDao.listarTipos(em).stream().map(TipoPropiedad::getNombre).toList());
            assertEquals(5, catalogoDao.listarEstados(em).size());
            assertEquals(2, catalogoDao.listarOperaciones(em).size());
            assertTrue(catalogoDao.operacionPorNombre(em, Operacion.VENTA).isPresent());
            assertTrue(catalogoDao.tipoPorNombre(em, "Departamento").isEmpty());
            assertEquals(zona.barrio(), catalogoDao.barrioPorId(em, zona.idBarrio()).orElseThrow().getNombre());
            assertTrue(catalogoDao.barrioPorId(em, -1).isEmpty());
        });
    }

    @Test
    void buscaUsuarioActivoSinDistinguirMayusculas() {
        enTransaccionDescartada(em -> {
            Rol admin = catalogoDao.rolPorNombre(em, "ADMIN").orElseThrow();
            String hash = "$2a$12$" + "a".repeat(53); // formato BCrypt; aquí no se verifica la contraseña
            em.persist(new Usuario("Prueba", "Dao", "prueba.dao@test.com", hash, admin));
            em.flush();

            assertTrue(usuarioDao.buscarActivoPorEmail(em, "PRUEBA.DAO@test.com").isPresent());
            assertTrue(usuarioDao.buscarActivoPorEmail(em, "otro.inexistente@test.com").isEmpty());
        });
    }
}

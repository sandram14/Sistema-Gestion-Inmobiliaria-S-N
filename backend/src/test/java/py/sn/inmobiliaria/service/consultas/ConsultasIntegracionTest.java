package py.sn.inmobiliaria.service.consultas;

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
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.repository.impl.JpaConsultaAdminDao;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Historial y visibilidad por rol contra la base real (requiere las migraciones 002 y 003).
 * Crea su propio vendedor, dos propiedades y cuatro consultas; los borra al terminar.
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ConsultasIntegracionTest {

    private static HikariDataSource ds;
    private static JpaProvider jpa;
    private static ConsultaAdminService servicio;
    private final String marca = UUID.randomUUID().toString().substring(0, 8);
    private final UUID sesion = UUID.randomUUID();
    private int vendedor;
    private long suya;
    private long ajena;
    private final long[] consultas = new long[4];

    @BeforeAll
    static void iniciar() {
        ds = DataSourceFactory.crear(new BaseDatosConfig(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(ds);
        servicio = new ConsultaAdminService(new TxRunner(jpa), new JpaConsultaAdminDao(), List::of);
    }

    @AfterAll
    static void cerrar() {
        if (jpa != null) jpa.close();
        if (ds != null) ds.close();
    }

    private long n(String sql, Object... p) {
        return DatosPrueba.numero(ds, sql, p);
    }

    /** Un vendedor de prueba, una propiedad suya, una ajena y 4 consultas de una misma conversación. */
    private void preparar() {
        vendedor = (int) n("INSERT INTO usuario (nombre, apellido, email, password_hash, id_rol) SELECT 'V', 'Prueba', ?, "
                + "'$2a$12$' || repeat('a', 53), id_rol FROM rol WHERE nombre = 'VENDEDOR' RETURNING id_usuario", "v." + marca + "@prueba.test");
        // Copias de una propiedad existente con códigos de prueba (los triggers validan ubicación y tipo)
        String copia = "INSERT INTO propiedad (codigo, titulo, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio, id_responsable) "
                + "SELECT ?, ?, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio, ? FROM propiedad ORDER BY id_propiedad LIMIT 1 RETURNING id_propiedad";
        suya = n(copia, "test-" + (900 + (int) (Math.random() * 99)) + "1", "Suya " + marca, vendedor);
        ajena = n(copia.replace("?) FROM", "NULL) FROM").replace(", ? FROM", ", NULL FROM"), "test-" + (900 + (int) (Math.random() * 99)) + "2", "Ajena " + marca);
        String c = "INSERT INTO consulta (id_sesion, consulta, respuesta, id_propiedad, resultado) VALUES (?, ?, 'r', ?, ?) RETURNING id_consulta";
        consultas[0] = n(c, sesion, "General " + marca, null, null);          // general (antigua: sin resultado)
        consultas[1] = n(c, sesion, "De la suya " + marca, suya, "RESPONDIDA");
        consultas[2] = n(c, sesion, "De la ajena " + marca, ajena, "RESPONDIDA");
        consultas[3] = n(c, sesion, "Ajena que recomienda la suya " + marca, ajena, "RESPONDIDA");
        n("WITH x AS (INSERT INTO consulta_recomendacion VALUES (?, ?) RETURNING 1) SELECT count(*) FROM x", consultas[3], suya);
    }

    @AfterEach
    void limpiar() {
        n("WITH x AS (DELETE FROM consulta WHERE consulta LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca);
        n("WITH x AS (DELETE FROM propiedad WHERE titulo LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca);
        n("WITH x AS (DELETE FROM usuario WHERE email LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca + "%");
    }

    private List<Long> idsVisibles(UsuarioSesion actor) {
        var pagina = servicio.historial(marca, null, null, null, null, 1, actor);
        assertEquals(pagina.elementos().size(), pagina.total());  // la cuenta usa el mismo filtro que la página
        return pagina.elementos().stream().map(r -> r.id()).sorted().toList();
    }

    @Test
    void cadaRolVeLasConsultasQueLeCorresponden() {
        preparar();
        UsuarioSesion v = new UsuarioSesion(vendedor, "V", "P", "v@x", "VENDEDOR");
        UsuarioSesion g = new UsuarioSesion(1, "G", "P", "g@x", "GERENTE");

        assertEquals(List.of(consultas[0], consultas[1], consultas[3]), idsVisibles(v)); // general, la suya y la que la recomienda
        assertEquals(4, idsVisibles(g).size());
        assertThrows(NoEncontradoException.class, () -> servicio.detalle(consultas[2], v)); // la ajena: 404
        assertEquals(3, servicio.detalle(consultas[1], v).conversacion().size());           // la conversación, sin la ajena
    }

    @Test
    void laConsultaAntiguaNoTieneDatosInventados() {
        preparar();
        var d = servicio.detalle(consultas[0], new UsuarioSesion(1, "A", "P", "a@x", "ADMIN"));
        assertFalse(d.registroCompleto());
        assertNull(d.resultado());
        assertTrue(d.recomendadas().isEmpty());
    }

    @Test
    void laRevisionInternaRegistraQuienYCuando() {
        preparar();
        int admin = (int) n("SELECT min(u.id_usuario) FROM usuario u JOIN rol r USING (id_rol) WHERE r.nombre = 'ADMIN'");
        UsuarioSesion a = new UsuarioSesion(admin, "A", "P", "a@x", "ADMIN");
        var revisada = servicio.revisar(consultas[1], "REVISADA", a);
        assertEquals("REVISADA", revisada.estadoRevision());
        assertNotNull(revisada.revisadaPor());
        assertNotNull(revisada.revisadaEn());
        var pendiente = servicio.revisar(consultas[1], "PENDIENTE", a);
        assertNull(pendiente.revisadaPor());
    }
}

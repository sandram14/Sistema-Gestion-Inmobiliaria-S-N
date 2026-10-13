package py.sn.inmobiliaria.service.clientes;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.*;
import py.sn.inmobiliaria.dto.ClienteDtos;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Clientes potenciales contra la base real (requiere la migración 005). Borra al terminar lo que crea. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class ClientesIntegracionTest {

    private static HikariDataSource ds;
    private static JpaProvider jpa;
    private static ClientePotencialService servicio;
    private final String marca = UUID.randomUUID().toString().substring(0, 8);
    private int vendedor;
    private String codigo;

    @BeforeAll
    static void iniciar() {
        ds = DataSourceFactory.crear(new BaseDatosConfig(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(ds);
        servicio = new ClientePotencialService(new TxRunner(jpa));
    }

    @AfterAll
    static void cerrar() {
        if (jpa != null) jpa.close();
        if (ds != null) ds.close();
    }

    private long n(String sql, Object... p) {
        return DatosPrueba.numero(ds, sql, p);
    }

    @BeforeEach
    void preparar() {
        vendedor = (int) n("INSERT INTO usuario (nombre, apellido, email, password_hash, id_rol) SELECT 'V', 'Prueba', ?, "
                + "'$2a$12$' || repeat('a', 53), id_rol FROM rol WHERE nombre = 'VENDEDOR' RETURNING id_usuario", "v." + marca + "@prueba.test");
        codigo = "test-" + (9000 + (int) (Math.random() * 999));
        n("INSERT INTO propiedad (codigo, titulo, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio, id_responsable) "
                + "SELECT ?, ?, latitud, longitud, id_tipo_propiedad, id_estado, id_barrio, ? FROM propiedad ORDER BY id_propiedad LIMIT 1 RETURNING id_propiedad",
                codigo, "Prop " + marca, vendedor);
    }

    @AfterEach
    void limpiar() {
        n("WITH x AS (DELETE FROM cliente_potencial WHERE nombre LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca);
        n("WITH x AS (DELETE FROM consulta WHERE consulta LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca);
        n("WITH x AS (DELETE FROM propiedad WHERE titulo LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca);
        n("WITH x AS (DELETE FROM usuario WHERE email LIKE ? RETURNING 1) SELECT count(*) FROM x", "%" + marca + "%");
    }

    @Test
    void desdeElChat_RegistraLaAceptacion_AsignaAlResponsableDeLaPropiedadYVinculaLaConsulta() {
        UUID sesion = UUID.randomUUID();
        long consulta = n("INSERT INTO consulta (id_sesion, consulta, respuesta) VALUES (?, ?, 'r') RETURNING id_consulta", sesion, "Pregunta " + marca);
        servicio.registrarDesdeChat(new ClienteDtos.Solicitud("Ana " + marca, "0981123456", null, "Quiero visitarla", codigo, true,
                ClientePotencialService.VERSION_CONSENTIMIENTO), sesion);

        assertEquals(1, n("SELECT count(*) FROM cliente_potencial WHERE nombre = ? AND origen = 'CHAT' AND estado = 'PENDIENTE' "
                + "AND id_responsable = ? AND id_consulta = ? AND consentimiento_en IS NOT NULL AND consentimiento_version = ?",
                "Ana " + marca, vendedor, consulta, ClientePotencialService.VERSION_CONSENTIMIENTO));
    }

    @Test
    void elVendedorSoloVeYActualizaLosAsignadosAEl() {
        UsuarioSesion v = new UsuarioSesion(vendedor, "V", "P", "v@x", "VENDEDOR");
        UsuarioSesion g = new UsuarioSesion(1, "G", "P", "g@x", "GERENTE");
        var suyo = servicio.registrarManual(new ClienteDtos.Manual("Suyo " + marca, "0981000000", null, null, null, "WHATSAPP", null), v);
        var ajeno = servicio.registrarManual(new ClienteDtos.Manual("Ajeno " + marca, null, "a@prueba.test", null, null, "LLAMADA", null), g);

        assertEquals(vendedor, suyo.responsable().id());                // lo que registra el vendedor queda a su nombre
        assertNull(ajeno.responsable());                                // sin propiedad: sin asignar
        assertEquals(1, servicio.listar(marca, null, null, 1, v).total());
        assertThrows(NoEncontradoException.class, () -> servicio.cambiarEstado(ajeno.id(), "CONTACTADO", v));
        assertEquals("CONTACTADO", servicio.cambiarEstado(suyo.id(), "CONTACTADO", v).estado());
        assertEquals(vendedor, servicio.asignar(ajeno.id(), vendedor, g).responsable().id()); // el gerente asigna
        var conteos = servicio.listar(marca, null, null, 1, g).conteos();
        assertEquals(2, conteos.total());
        assertEquals(1, conteos.contactados());
    }
}

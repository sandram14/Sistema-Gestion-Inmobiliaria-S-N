package py.sn.inmobiliaria.service.asistente;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.AsistenteConfig;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.response.AsistenteResponse;
import py.sn.inmobiliaria.exception.AsistenteNoDisponibleException;
import py.sn.inmobiliaria.integration.ErrorClienteIa;
import py.sn.inmobiliaria.integration.GeminiClient;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.repository.impl.JpaConsultaDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asistente contra la base real (solo si DB_URL está definida).
 *
 * <ul>
 *   <li>Por defecto usa un cliente de IA simulado: verifica el contexto leído de la base y el
 *       registro en CONSULTA sin consumir la cuota de Gemini.</li>
 *   <li>La prueba con Gemini REAL solo se ejecuta si además GEMINI_API_KEY está definida y
 *       PROBAR_GEMINI_REAL=true: hace una única consulta. Nunca contra la base sn_inmobiliaria
 *       (se omite): se ejecuta en el entorno de pruebas (docs/pruebas.md).</li>
 * </ul>
 * Al terminar borra las consultas que creó.
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class AsistenteIntegracionTest {

    /** Nombre de la base del sistema real (DB_NAME de docker/.env.example). */
    static final String BASE_REAL = "sn_inmobiliaria";

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static TxRunner tx;
    private final UUID visitante = UUID.randomUUID();

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        tx = new TxRunner(jpa);
    }

    @AfterEach
    void borrarConsultas() {
        DatosPrueba.numero(dataSource,
                "WITH b AS (DELETE FROM consulta WHERE id_sesion = ? RETURNING 1) SELECT count(*) FROM b", visitante);
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

    private static AsistenteService servicio(py.sn.inmobiliaria.integration.ClienteIa cliente) {
        AsistenteConfig config = AsistenteConfig.desde(System.getenv());
        return new AsistenteService(tx, new JpaPropiedadDao(), new JpaConsultaDao(), cliente,
                new LimitadorAsistente(100, 100, 100, 100, Clock.systemUTC()), config);
    }

    /** Nombre de la base de una URL JDBC (jdbc:postgresql://host:puerto/base?opciones → base). */
    static String baseDeDatos(String url) {
        String sinOpciones = url.split("\\?", 2)[0];
        return sinOpciones.substring(sinOpciones.lastIndexOf('/') + 1);
    }

    private long consultasDelVisitante() {
        return DatosPrueba.numero(dataSource, "SELECT count(*) FROM consulta WHERE id_sesion = ?", visitante);
    }

    /** Código de cualquier propiedad disponible, o null si no hay ninguna. */
    private static String algunaPropiedadDisponible() {
        try (var con = dataSource.getConnection();
             var ps = con.prepareStatement("SELECT p.codigo FROM propiedad p JOIN estado_propiedad e USING (id_estado) "
                     + "WHERE e.nombre = 'Disponible' ORDER BY p.codigo LIMIT 1");
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void consultaSobreUnaPropiedadRealQuedaRegistradaConSuPropiedad() {
        String codigo = algunaPropiedadDisponible();
        if (codigo == null) {
            return;
        }
        String[] contenido = new String[1];
        AsistenteService s = servicio((instrucciones, c) -> {
            contenido[0] = c;
            return "Respuesta simulada sobre " + codigo + ". [[AGENTE]]";
        });

        AsistenteResponse r = s.responder(visitante, "¿Tiene cochera?", codigo);

        assertTrue(contenido[0].contains("Código: " + codigo));
        assertTrue(r.sugerirWhatsApp());
        assertTrue(r.propiedadesRelacionadas().contains(codigo));
        assertEquals(1, consultasDelVisitante());
        assertEquals(1, DatosPrueba.numero(dataSource,
                "SELECT count(*) FROM consulta c JOIN propiedad p USING (id_propiedad) WHERE c.id_sesion = ? AND p.codigo = ?",
                visitante, codigo));
    }

    @Test
    void consultaGeneralUsaSoloPropiedadesDisponibles() {
        String[] contenido = new String[1];
        AsistenteService s = servicio((instrucciones, c) -> {
            contenido[0] = c;
            return "Respuesta simulada.";
        });

        s.responder(visitante, "¿Qué propiedades tienen?", null);

        long disponibles = DatosPrueba.numero(dataSource, "SELECT count(*) FROM propiedad p JOIN estado_propiedad e "
                + "USING (id_estado) WHERE e.nombre = 'Disponible'");
        long enviadas = Math.min(disponibles, AsistenteConfig.desde(Map.of()).maxPropiedadesContexto());
        assertTrue(contenido[0].contains("Propiedades disponibles (" + enviadas));
        assertEquals(1, consultasDelVisitante());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "PROBAR_GEMINI_REAL", matches = "(?i)true")
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void unaConsultaRealAGemini() {
        // Consume cuota de Gemini y escribe (y luego borra) una fila en CONSULTA: nunca contra la base real
        String base = baseDeDatos(System.getenv("DB_URL"));
        Assumptions.assumeFalse(BASE_REAL.equalsIgnoreCase(base), () -> "La prueba con Gemini real no se ejecuta contra la base "
                + BASE_REAL + ": usar la base del entorno de pruebas (docs/pruebas.md).");
        AsistenteConfig config = AsistenteConfig.desde(System.getenv());
        GeminiClient gemini = new GeminiClient(config.apiKey(), config.modelo(),
                config.nivelRazonamiento(), Duration.ofSeconds(config.timeoutSegundos()));
        ErrorClienteIa[] error = new ErrorClienteIa[1];
        AsistenteService s = servicio((instrucciones, contenido) -> {
            try {
                return gemini.generar(instrucciones, contenido);
            } catch (ErrorClienteIa e) {
                error[0] = e;
                throw e;
            }
        });

        AsistenteResponse r;
        try {
            r = s.responder(visitante, "¿Qué tipos de propiedades tienen disponibles? Responde en una frase.", null);
        } catch (AsistenteNoDisponibleException e) {
            // Saturación o cuota de Google: no es un error del proyecto → la prueba se OMITE con el motivo.
            // Un error de configuración (clave o modelo) sí la hace FALLAR.
            boolean temporal = error[0] != null && (error[0].tipo() == ErrorClienteIa.Tipo.NO_DISPONIBLE
                    || error[0].tipo() == ErrorClienteIa.Tipo.CUOTA_AGOTADA);
            Assumptions.assumeFalse(temporal, () -> "Gemini no disponible temporalmente (" + error[0].getMessage()
                    + "). Reintente la prueba más tarde.");
            throw e;
        }

        assertFalse(r.respuesta().isBlank());
        assertFalse(r.respuesta().contains(AsistenteService.MARCA_AGENTE));
        assertEquals(1, consultasDelVisitante());
        System.out.println("Respuesta de Gemini (" + config.modelo() + "): " + r.respuesta());
    }
}

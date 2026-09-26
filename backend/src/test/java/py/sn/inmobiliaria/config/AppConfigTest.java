package py.sn.inmobiliaria.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void sinVariablesUsaValoresPorDefecto() {
        AppConfig config = AppConfig.desde(Map.of(), new Properties());

        assertEquals("sn-inmobiliaria-backend", config.nombre());
        assertEquals("desconocida", config.version());
        assertEquals("desarrollo", config.entorno());
        assertFalse(config.esProduccion());
    }

    @Test
    void leeVersionDesdeAppProperties() {
        Properties props = new Properties();
        props.setProperty("app.version", "0.1.0");

        AppConfig config = AppConfig.desde(Map.of(), props);

        assertEquals("0.1.0", config.version());
    }

    @Test
    void leeEntornoDesdeVariableAppEnv() {
        AppConfig config = AppConfig.desde(Map.of("APP_ENV", " produccion "), new Properties());

        assertEquals("produccion", config.entorno());
        assertTrue(config.esProduccion());
    }

    @Test
    void appEnvVacioSeTrataComoNoDefinido() {
        AppConfig config = AppConfig.desde(Map.of("APP_ENV", "   "), new Properties());

        assertEquals("desarrollo", config.entorno());
    }

    @Test
    void conLasTresVariablesDbLaBaseQuedaConfigurada() {
        AppConfig config = AppConfig.desde(Map.of(
                "DB_URL", "jdbc:postgresql://localhost:5433/sn_inmobiliaria",
                "DB_USER", "sn_app",
                "DB_PASSWORD", "secreta"), new Properties());

        assertTrue(config.baseDatos().isPresent());
        assertEquals("sn_app", config.baseDatos().get().usuario());
    }

    @Test
    void siFaltaLaContrasenaLaBaseNoQuedaConfigurada() {
        AppConfig config = AppConfig.desde(Map.of(
                "DB_URL", "jdbc:postgresql://localhost:5433/sn_inmobiliaria",
                "DB_USER", "sn_app"), new Properties());

        assertTrue(config.baseDatos().isEmpty());
    }

    @Test
    void laContrasenaNuncaApareceEnToString() {
        BaseDatosConfig bd = new BaseDatosConfig("jdbc:postgresql://db/x", "sn_app", "secreta");

        assertFalse(bd.toString().contains("secreta"));
    }
}

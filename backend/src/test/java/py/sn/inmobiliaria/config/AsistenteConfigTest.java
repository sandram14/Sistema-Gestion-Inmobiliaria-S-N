package py.sn.inmobiliaria.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsistenteConfigTest {

    @Test
    void valoresPorDefectoPensadosParaElNivelGratuito() {
        AsistenteConfig c = AsistenteConfig.desde(Map.of());

        assertNull(c.apiKey());
        assertFalse(c.configurado());
        assertEquals("gemini-3.1-flash-lite", c.modelo());
        assertEquals("minimal", c.nivelRazonamiento());
        assertEquals(10, c.limiteMinutoSesion());
        assertEquals(8, c.limiteMinutoGlobal());
        assertEquals(200, c.limiteDiaGlobal());
    }

    @Test
    void leeLasVariablesDeEntorno() {
        AsistenteConfig c = AsistenteConfig.desde(Map.of("GEMINI_API_KEY", " clave ", "GEMINI_MODEL", "gemini-3.5-flash",
                "ASISTENTE_LIMITE_MINUTO_GLOBAL", "4", "ASISTENTE_LIMITE_DIA_GLOBAL", "abc"));

        assertTrue(c.configurado());
        assertEquals("clave", c.apiKey());
        assertEquals("gemini-3.5-flash", c.modelo());
        assertEquals(4, c.limiteMinutoGlobal());
        assertEquals(200, c.limiteDiaGlobal()); // valor inválido → por defecto
    }

    @Test
    void nivelDeRazonamientoConfigurable() {
        assertEquals("low", AsistenteConfig.desde(Map.of("GEMINI_THINKING_LEVEL", "LOW")).nivelRazonamiento());
        assertNull(AsistenteConfig.desde(Map.of("GEMINI_THINKING_LEVEL", "ninguno")).nivelRazonamiento());
    }

    @Test
    void laClaveNuncaApareceEnToString() {
        String texto = AsistenteConfig.desde(Map.of("GEMINI_API_KEY", "clave-falsa-de-prueba-123")).toString();

        assertFalse(texto.contains("clave-falsa-de-prueba-123"));
        assertTrue(texto.contains("****"));
    }
}

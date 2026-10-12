package py.sn.inmobiliaria.seguridad;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaContrasenaTest {

    @Test
    void reglasDeLaContrasena() {
        assertNotNull(PoliticaContrasena.problema(null));
        assertNotNull(PoliticaContrasena.problema("abc123"));              // corta
        assertNotNull(PoliticaContrasena.problema("soloLetrasLargas"));    // sin número
        assertNotNull(PoliticaContrasena.problema("1234567890"));          // sin letra
        assertNotNull(PoliticaContrasena.problema("a1" + "x".repeat(80))); // más de 72 bytes
        assertNull(PoliticaContrasena.problema("casaVerde2026"));
    }

    @Test
    void lasTemporalesCumplenLasReglasYNoSeRepiten() {
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String t = PoliticaContrasena.generarTemporal();
            assertEquals(12, t.length());
            assertNull(PoliticaContrasena.problema(t), t);
            assertTrue(t.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0), t); // sin caracteres que se confunden
            vistas.add(t);
        }
        assertEquals(200, vistas.size());
    }
}

package py.sn.inmobiliaria.service.asistente;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El seguro de la prueba con Gemini real (AsistenteIntegracionTest) reconoce la base de la URL:
 * con la base real se omite, con la del entorno de pruebas se ejecuta.
 */
class SeguroGeminiRealTest {

    @Test
    void reconoceLaBaseRealEnCualquierFormaDeLaUrl() {
        assertEquals(AsistenteIntegracionTest.BASE_REAL,
                AsistenteIntegracionTest.baseDeDatos("jdbc:postgresql://localhost:5433/sn_inmobiliaria"));
        assertEquals(AsistenteIntegracionTest.BASE_REAL,
                AsistenteIntegracionTest.baseDeDatos("jdbc:postgresql://db:5432/sn_inmobiliaria?sslmode=disable"));
    }

    @Test
    void laBaseDelEntornoDePruebasNoEsLaReal() {
        assertEquals("sn_prueba", AsistenteIntegracionTest.baseDeDatos("jdbc:postgresql://localhost:5434/sn_prueba"));
    }
}

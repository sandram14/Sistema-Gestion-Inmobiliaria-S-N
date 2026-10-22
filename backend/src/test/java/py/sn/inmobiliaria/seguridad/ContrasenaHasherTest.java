package py.sn.inmobiliaria.seguridad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContrasenaHasherTest {

    private final ContrasenaHasher hasher = new ContrasenaHasher(4); // costo bajo: pruebas rápidas

    @Test
    void hashearYVerificar() {
        String hash = hasher.hashear("Mi-Clave-2026");

        assertTrue(hash.startsWith("$2a$04$"));
        assertTrue(hasher.verificar("Mi-Clave-2026", hash));
        assertFalse(hasher.verificar("mi-clave-2026", hash)); // distingue mayúsculas
    }

    @Test
    void laMismaContrasenaGeneraHashesDistintos() {
        assertNotEquals(hasher.hashear("igual"), hasher.hashear("igual")); // sal aleatoria
    }

    @Test
    void reconoceLosHashesQueGeneraPgcrypto() {
        // Generado por PostgreSQL: SELECT crypt('Clave-De-Prueba', gen_salt('bf', 4))
        String hashDePostgres = "$2a$04$RmGwEcbvSgSSmiU0E2XJwelZ4Th0/yIrYFhZHxm1QUI/BI7S2TSVG";

        assertTrue(hasher.verificar("Clave-De-Prueba", hashDePostgres));
        assertFalse(hasher.verificar("otra", hashDePostgres));
    }

    @Test
    void datosInvalidosNuncaLanzanExcepcion() {
        assertFalse(hasher.verificar(null, "$2a$04$x"));
        assertFalse(hasher.verificar("clave", null));
        assertFalse(hasher.verificar("clave", "no-es-un-hash"));
    }
}

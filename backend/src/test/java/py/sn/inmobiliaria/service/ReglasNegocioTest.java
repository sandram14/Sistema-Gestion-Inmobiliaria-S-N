package py.sn.inmobiliaria.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasNegocioTest {

    @Test
    void operacionesPermitidasPorTipo() {
        assertTrue(ReglasNegocio.operacionPermitida("Casa", "Venta"));
        assertTrue(ReglasNegocio.operacionPermitida("Casa", "Alquiler"));
        assertTrue(ReglasNegocio.operacionPermitida("Terreno", "Venta"));
        assertFalse(ReglasNegocio.operacionPermitida("Terreno", "Alquiler"));
        assertEquals(List.of("Venta"), ReglasNegocio.operacionesPorTipo().get("Terreno"));
    }

    @Test
    void codigoPublicoSegunTipo() {
        assertEquals("prop-017", ReglasNegocio.formatearCodigo(ReglasNegocio.prefijoCodigo("Casa"), 17));
        assertEquals("terr-013", ReglasNegocio.formatearCodigo(ReglasNegocio.prefijoCodigo("Terreno"), 13));
        assertEquals("prop-1000", ReglasNegocio.formatearCodigo("prop", 1000));
    }
}

package py.sn.inmobiliaria.service.validacion;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.CasaRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.OperacionRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.TerrenoRequest;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Moneda;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorPropiedadTest {

    private static final CasaRequest CASA = new CasaRequest(3, 2, true, true, null, new BigDecimal("182"), null);
    private static final TerrenoRequest TERRENO = new TerrenoRequest(new BigDecimal("750"), new BigDecimal("15"), null, true);

    private static OperacionRequest op(String operacion, String precio, String moneda) {
        return new OperacionRequest(operacion, new BigDecimal(precio), moneda);
    }

    private static PropiedadRequest casa(List<OperacionRequest> ops) {
        return new PropiedadRequest("  Casa en Area 1  ", "", null,
                new BigDecimal("-25.51170049"), new BigDecimal("-54.6118"), 10011013,
                "casa", null, CASA, null, ops);
    }

    private static PropiedadRequest terreno(List<OperacionRequest> ops) {
        return new PropiedadRequest("Terreno", null, null, new BigDecimal("-25.4"), new BigDecimal("-54.7"),
                10051007, "Terreno", "disponible", null, TERRENO, ops);
    }

    @Test
    void casaValidaQuedaNormalizada() {
        PropiedadValidada v = ValidadorPropiedad.validar(casa(List.of(
                op("venta", "120000", "usd"), op("Alquiler", "4000000", "PYG"))));

        assertEquals("Casa en Area 1", v.titulo());          // sin espacios sobrantes
        assertNull(v.descripcion());                         // texto vacío -> null
        assertEquals("Casa", v.tipo());
        assertNull(v.estado());                              // no indicado
        assertEquals(new BigDecimal("-25.511700"), v.latitud()); // 6 decimales, como numeric(9,6)
        assertEquals((short) 3, v.casa().habitaciones());
        assertFalse(v.casa().cocina());                      // null -> false
        assertNull(v.terreno());
        assertEquals("Venta", v.operaciones().get(0).operacion());
        assertEquals(Moneda.USD, v.operaciones().get(0).moneda());
        assertEquals(Moneda.PYG, v.operaciones().get(1).moneda());
    }

    @Test
    void terrenoValidoConEstado() {
        PropiedadValidada v = ValidadorPropiedad.validar(terreno(List.of(op("Venta", "41000", "USD"))));

        assertEquals("Terreno", v.tipo());
        assertEquals("Disponible", v.estado());
        assertTrue(v.terreno().tieneTitulo());
        assertNull(v.casa());
    }

    @Test
    void terrenoEnAlquilerSeRechaza() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(
                terreno(List.of(op("Venta", "41000", "USD"), op("Alquiler", "1000000", "PYG")))));

        assertEquals("Un terreno solamente puede tener la operación Venta", e.campos().get("operaciones[1].operacion"));
    }

    @Test
    void exigeAlMenosUnaOperacion() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(casa(List.of())));

        assertTrue(e.campos().containsKey("operaciones"));
    }

    @Test
    void rechazaOperacionRepetidaYPrecioCero() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(
                casa(List.of(op("Venta", "1", "USD"), op("venta", "0", "USD")))));

        assertTrue(e.campos().containsKey("operaciones[1].operacion"));
        assertTrue(e.campos().containsKey("operaciones[1].precio"));
    }

    @Test
    void elSubtipoDebeCoincidirConElTipo() {
        PropiedadRequest casaConTerreno = new PropiedadRequest("Casa", null, null, BigDecimal.ONE, BigDecimal.ONE,
                1, "Casa", null, null, TERRENO, List.of(op("Venta", "1", "USD")));

        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(casaConTerreno));

        assertTrue(e.campos().containsKey("casa"));      // faltan los datos de casa
        assertTrue(e.campos().containsKey("terreno"));   // sobran los de terreno
    }

    @Test
    void enElPanelNoSeAceptaViviendaNiDepartamento() {
        for (String tipo : List.of("Vivienda", "Departamento")) {
            PropiedadRequest r = new PropiedadRequest("X", null, null, BigDecimal.ONE, BigDecimal.ONE, 1, tipo,
                    null, CASA, null, List.of(op("Venta", "1", "USD")));
            ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(r));
            assertTrue(e.campos().containsKey("tipo"), tipo);
        }
    }

    @Test
    void informaTodosLosErroresDeUnFormularioVacio() {
        PropiedadRequest vacio = new PropiedadRequest(null, null, null, null, null, null, null, null, null, null, null);

        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(vacio));

        assertTrue(e.campos().keySet().containsAll(
                List.of("titulo", "latitud", "longitud", "idBarrio", "tipo", "operaciones")));
    }

    @Test
    void rechazaCoordenadasFueraDeRango() {
        PropiedadRequest r = new PropiedadRequest("X", null, null, new BigDecimal("95"), new BigDecimal("-200"), 1,
                "Casa", null, CASA, null, List.of(op("Venta", "1", "USD")));

        ValidacionException e = assertThrows(ValidacionException.class, () -> ValidadorPropiedad.validar(r));

        assertTrue(e.campos().containsKey("latitud"));
        assertTrue(e.campos().containsKey("longitud"));
    }
}

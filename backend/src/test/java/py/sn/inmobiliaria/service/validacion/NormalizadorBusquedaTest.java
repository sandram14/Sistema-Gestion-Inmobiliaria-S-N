package py.sn.inmobiliaria.service.validacion;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.repository.FiltroPropiedades;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizadorBusquedaTest {

    private static FiltroPropiedades normalizar(String categoria, String operacion, String presupuesto, String moneda) {
        return NormalizadorBusqueda.normalizar(new BusquedaRequest(null, null, categoria, operacion,
                presupuesto == null ? null : new BigDecimal(presupuesto), moneda));
    }

    @Test
    void sinCriteriosNoFiltraNada() {
        FiltroPropiedades f = NormalizadorBusqueda.normalizar(BusquedaRequest.vacia());

        assertNull(f.tipo());
        assertNull(f.operacion());
        assertNull(f.presupuestoMax());
    }

    @Test
    void aceptaElTerminoViviendaDelFrontendYMayusculasIndistintas() {
        FiltroPropiedades f = normalizar("vivienda", "ALQUILER", null, null);

        assertEquals("Casa", f.tipo());
        assertEquals("Alquiler", f.operacion());
    }

    @Test
    void rechazaTerrenoEnAlquilerAunqueElFrontendYaLoEvite() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> normalizar("Terreno", "Alquiler", null, null));

        assertTrue(e.campos().containsKey("operacion"));
    }

    @Test
    void presupuestoSinMonedaUsaUsdPorDefecto() {
        assertEquals(Moneda.USD, normalizar(null, "Venta", "50000", null).moneda());
        assertEquals(Moneda.PYG, normalizar(null, "Venta", "50000", "pyg").moneda());
    }

    @Test
    void informaTodosLosErroresJuntos() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> normalizar("Departamento", "Permuta", "-1", "EUR"));

        assertEquals(4, e.campos().size());
        assertTrue(e.campos().keySet().containsAll(java.util.Set.of("categoria", "operacion", "presupuestoMax", "moneda")));
    }

    @Test
    void pasaElDepartamentoAlFiltro() {
        FiltroPropiedades f = NormalizadorBusqueda.normalizar(
                new BusquedaRequest(1, null, null, null, null, null, 1));

        assertEquals(1, f.departamentoId());
        assertEquals(1, f.ciudadId());
        assertNull(NormalizadorBusqueda.normalizar(BusquedaRequest.vacia()).departamentoId());
    }

    @Test
    void rechazaDepartamentoNoPositivo() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> NormalizadorBusqueda.normalizar(new BusquedaRequest(null, null, null, null, null, null, 0)));

        assertTrue(e.campos().containsKey("departamentoId"));
    }

    @Test
    void rechazaIdsNoPositivos() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> NormalizadorBusqueda.normalizar(new BusquedaRequest(0, -5, null, null, null, null)));

        assertTrue(e.campos().containsKey("ciudadId"));
        assertTrue(e.campos().containsKey("barrioId"));
    }

    @Test
    void presupuestoMinimoYMaximoEnLaMismaMoneda() {
        var f = NormalizadorBusqueda.normalizar(new BusquedaRequest(null, null, null, null,
                new java.math.BigDecimal("900000000"), "PYG", null, new java.math.BigDecimal("300000000")));
        assertEquals(new java.math.BigDecimal("300000000"), f.presupuestoMin());
        assertEquals(py.sn.inmobiliaria.model.Moneda.PYG, f.moneda());
        assertTrue(f.tienePresupuesto());
        var soloMinimo = NormalizadorBusqueda.normalizar(new BusquedaRequest(null, null, null, null, null, null, null, new java.math.BigDecimal("50000")));
        assertEquals(py.sn.inmobiliaria.model.Moneda.USD, soloMinimo.moneda());   // USD por defecto, como con el máximo
    }

    @Test
    void elMinimoNoPuedeSuperarAlMaximoNiSerCero() {
        var invertido = assertThrows(py.sn.inmobiliaria.exception.ValidacionException.class, () -> NormalizadorBusqueda.normalizar(
                new BusquedaRequest(null, null, null, null, new java.math.BigDecimal("100"), "USD", null, new java.math.BigDecimal("200"))));
        assertTrue(invertido.campos().containsKey("presupuestoMin"));
        assertThrows(py.sn.inmobiliaria.exception.ValidacionException.class, () -> NormalizadorBusqueda.normalizar(
                new BusquedaRequest(null, null, null, null, null, "USD", null, java.math.BigDecimal.ZERO)));
    }
}

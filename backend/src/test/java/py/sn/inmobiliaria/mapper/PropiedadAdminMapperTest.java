package py.sn.inmobiliaria.mapper;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.dto.response.admin.PropiedadDetalleDto;
import py.sn.inmobiliaria.dto.response.admin.PropiedadResumenDto;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropiedadAdminMapperTest {

    @Test
    void resumenParaElListado() {
        PropiedadResumenDto r = PropiedadAdminMapper.resumen(Entidades.casaProp001());

        assertEquals(1L, r.id());
        assertEquals("prop-001", r.codigo());
        assertEquals("Disponible", r.estado());
        assertEquals("AREA 1", r.barrio());
        assertEquals("/images/propiedades/prop-001/1.png", r.imagenPrincipal());
        assertEquals(2, r.operaciones().size());
    }

    @Test
    void detalleParaElFormularioDeEdicion() {
        PropiedadDetalleDto d = PropiedadAdminMapper.detalle(Entidades.casaProp001());

        assertEquals("Casa", d.tipo());
        assertEquals(10011013, d.ubicacion().idBarrio());
        assertEquals(1, d.ubicacion().idCiudad());
        assertEquals("ALTO PARANÁ", d.ubicacion().departamento());
        assertEquals(3, d.casa().habitaciones());
        assertTrue(d.casa().cocina());
        assertEquals(new BigDecimal("182"), d.casa().superficieConstruida());
        assertNull(d.terreno());
        assertEquals(3, d.imagenes().size());
        assertTrue(d.imagenes().get(0).esPrincipal());
        assertEquals(100L, d.imagenes().get(0).id());
    }

    @Test
    void detalleDeUnTerreno() {
        PropiedadDetalleDto d = PropiedadAdminMapper.detalle(Entidades.terrenoSinDireccion());

        assertNull(d.casa());
        assertEquals(new BigDecimal("750"), d.terreno().superficie());
        assertNull(d.terreno().frente());
    }
}

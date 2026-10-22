package py.sn.inmobiliaria.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.repository.UbicacionDao.BarrioGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.CiudadGeo;
import py.sn.inmobiliaria.util.JsonUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica que el GeoJSON generado tenga exactamente la forma que espera el frontend. */
class GeoJsonMapperTest {

    /** Campos de PropiedadProps en frontend/src/components/PropertyMap/types.ts. */
    private static final Set<String> CAMPOS_FRONTEND = Set.of("id", "titulo", "categoria", "tipoOperacion", "ciudad",
            "barrio", "barrioId", "precio", "moneda", "dormitorios", "banos", "m2", "direccion", "fotos");

    private static JsonNode json(Object o) throws Exception {
        return JsonUtil.mapper().readTree(JsonUtil.mapper().writeValueAsString(o));
    }

    private static Set<String> campos(JsonNode n) {
        Set<String> s = new HashSet<>();
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
            s.add(it.next());
        }
        return s;
    }

    @Test
    void propiedadTieneLosCamposDelFrontendMasLosNuevos() throws Exception {
        JsonNode f = json(GeoJsonMapper.propiedad(null).apply(Entidades.casaProp001()));

        assertEquals("Feature", f.get("type").asText());
        Set<String> esperados = new HashSet<>(CAMPOS_FRONTEND);
        esperados.addAll(Set.of("operaciones", "descripcion"));
        assertEquals(esperados, campos(f.get("properties")));
    }

    @Test
    void geometriaEnOrdenLongitudLatitud() throws Exception {
        JsonNode geo = json(GeoJsonMapper.propiedad(null).apply(Entidades.casaProp001())).get("geometry");

        assertEquals("Point", geo.get("type").asText());
        assertEquals(-54.6118, geo.get("coordinates").get(0).asDouble(), 1e-9);
        assertEquals(-25.5117, geo.get("coordinates").get(1).asDouble(), 1e-9);
    }

    @Test
    void valoresDeUnaCasa() throws Exception {
        JsonNode p = json(GeoJsonMapper.propiedad(null).apply(Entidades.casaProp001())).get("properties");

        assertEquals("prop-001", p.get("id").asText());           // código público, no el id interno
        assertEquals("Casa", p.get("categoria").asText());
        assertEquals("CIUDAD DEL ESTE", p.get("ciudad").asText());
        assertEquals(10011013, p.get("barrioId").asInt());
        assertEquals(3, p.get("dormitorios").asInt());
        assertEquals("182", p.get("m2").toString());             // sin ".00"
        assertEquals(3, p.get("fotos").size());
        assertEquals("/images/propiedades/prop-001/1.png", p.get("fotos").get(0).asText());
        assertTrue(p.get("descripcion").isNull());
    }

    @Test
    void sinFiltroLaOperacionPrincipalEsVentaYLasOperacionesVanOrdenadas() throws Exception {
        JsonNode p = json(GeoJsonMapper.propiedad(null).apply(Entidades.casaProp001())).get("properties");

        assertEquals("Venta", p.get("tipoOperacion").asText());
        assertEquals("45000", p.get("precio").toString());       // número plano, sin notación científica
        assertEquals("PYG", p.get("moneda").asText());
        assertEquals("Venta", p.get("operaciones").get(0).get("operacion").asText());
        assertEquals("4000000", p.get("operaciones").get(1).get("precio").toString());
    }

    @Test
    void conFiltroAlquilerMuestraElPrecioDeAlquiler() throws Exception {
        JsonNode p = json(GeoJsonMapper.propiedad("Alquiler").apply(Entidades.casaProp001())).get("properties");

        assertEquals("Alquiler", p.get("tipoOperacion").asText());
        assertEquals("4000000", p.get("precio").toString());
        assertEquals("PYG", p.get("moneda").asText());
    }

    @Test
    void terrenoUsaSuperficieYCeroAmbientes() throws Exception {
        JsonNode p = json(GeoJsonMapper.propiedad(null).apply(Entidades.terrenoSinDireccion())).get("properties");

        assertEquals("Terreno", p.get("categoria").asText());
        assertEquals(0, p.get("dormitorios").asInt());
        assertEquals(0, p.get("banos").asInt());
        assertEquals("750", p.get("m2").toString());
        assertEquals("", p.get("direccion").asText());           // texto vacío, como espera el frontend
        assertEquals(0, p.get("fotos").size());
    }

    @Test
    void ciudadesComoPuntosConIdDeTexto() throws Exception {
        JsonNode fc = json(GeoJsonMapper.ciudades(List.of(
                new CiudadGeo(1, "CIUDAD DEL ESTE", "01", 1, "ALTO PARANÁ", -54.641961, -25.505118, 4))));

        assertEquals("FeatureCollection", fc.get("type").asText());
        JsonNode f = fc.get("features").get(0);
        // Los campos de types.ts más departamentoId (nuevo, el frontend actual lo ignora)
        assertEquals(Set.of("id", "ciudad", "departamento", "totalBarrios", "color", "departamentoId"),
                campos(f.get("properties")));
        assertEquals(1, f.get("properties").get("departamentoId").asInt());
        assertEquals("1", f.get("properties").get("id").asText());
        assertTrue(f.get("properties").get("id").isTextual());
        assertEquals(4, f.get("properties").get("totalBarrios").asInt());
        assertTrue(f.get("properties").get("color").asText().matches("#[0-9A-F]{6}"));
        assertEquals(-54.641961, f.get("geometry").get("coordinates").get(0).asDouble(), 1e-9);
    }

    @Test
    void barriosConLaGeometriaDePostgisSinModificar() throws Exception {
        String geojson = "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[-54.6,-25.5],[-54.61,-25.5],[-54.61,-25.51],[-54.6,-25.5]]]]}";
        JsonNode fc = json(GeoJsonMapper.barrios(List.of(
                new BarrioGeo(10011013, "AREA 1", 1, "CIUDAD DEL ESTE", "ALTO PARANÁ", geojson))));

        JsonNode f = fc.get("features").get(0);
        assertEquals("MultiPolygon", f.get("geometry").get("type").asText()); // objeto, no texto
        assertEquals(JsonUtil.mapper().readTree(geojson), f.get("geometry"));
        assertEquals(Set.of("id", "barrio", "distritoCod", "ciudad", "departamento", "color"), campos(f.get("properties")));
        assertEquals("1", f.get("properties").get("distritoCod").asText()); // = id de la ciudad
    }

    @Test
    void elMismoIdSiempreRecibeElMismoColor() {
        assertEquals(Formato.color(10011013), Formato.color(10011013));
    }
}

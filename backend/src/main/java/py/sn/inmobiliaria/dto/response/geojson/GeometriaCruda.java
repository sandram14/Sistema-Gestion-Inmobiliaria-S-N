package py.sn.inmobiliaria.dto.response.geojson;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Geometría que PostGIS ya entrega como texto GeoJSON (ST_AsGeoJSON).
 * Se inserta tal cual en la respuesta, sin volver a interpretarla: más rápido y sin
 * perder precisión en los polígonos de los barrios.
 *
 * <p>Es una clase (no un record) a propósito: en un record, Jackson vería la anotación
 * dos veces (en el campo y en el método) y rechazaría la definición.</p>
 */
public final class GeometriaCruda {

    private final String json;

    public GeometriaCruda(String json) {
        this.json = json;
    }

    @JsonValue
    @JsonRawValue
    public String json() {
        return json;
    }
}

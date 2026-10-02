package py.sn.inmobiliaria.dto.response.geojson;

import java.util.List;

/**
 * Colección GeoJSON: el mismo formato que leían los archivos estáticos del frontend
 * (frontend/public/data/*.geojson), así el mapa no necesita cambios.
 *
 * <pre>{ "type": "FeatureCollection", "features": [ ... ] }</pre>
 */
public record FeatureCollection<P>(String type, List<Feature<P>> features) {

    public static <P> FeatureCollection<P> de(List<Feature<P>> features) {
        return new FeatureCollection<>("FeatureCollection", List.copyOf(features));
    }
}

package py.sn.inmobiliaria.dto.response.geojson;

/**
 * Un elemento GeoJSON: geometría + propiedades.
 *
 * @param geometry {@link Punto} o {@link GeometriaCruda} (polígonos generados por PostGIS)
 */
public record Feature<P>(String type, Object geometry, P properties) {

    public static <P> Feature<P> de(Object geometry, P properties) {
        return new Feature<>("Feature", geometry, properties);
    }
}

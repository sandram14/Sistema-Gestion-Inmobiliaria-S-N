package py.sn.inmobiliaria.dto.response.geojson;

/**
 * Propiedades de una ciudad en el mapa. Coincide con {@code CiudadProps} de
 * frontend/src/components/PropertyMap/types.ts.
 *
 * @param id             identificador de la ciudad (como texto, igual que antes)
 * @param departamentoId id del departamento (campo nuevo; el frontend actual puede ignorarlo)
 */
public record CiudadProps(String id, String ciudad, String departamento, int totalBarrios, String color,
                          int departamentoId) {
}

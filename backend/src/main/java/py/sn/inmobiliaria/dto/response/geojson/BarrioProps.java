package py.sn.inmobiliaria.dto.response.geojson;

/**
 * Propiedades de un barrio en el mapa. Coincide con {@code BarrioProps} de types.ts.
 *
 * @param id          CLAVE_BAR oficial
 * @param distritoCod id de la ciudad a la que pertenece (el frontend lo compara con CiudadProps.id)
 */
public record BarrioProps(int id, String barrio, String distritoCod, String ciudad, String departamento, String color) {
}

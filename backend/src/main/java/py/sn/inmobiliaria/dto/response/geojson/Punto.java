package py.sn.inmobiliaria.dto.response.geojson;

/**
 * Geometría Point de GeoJSON. Atención al orden: GeoJSON usa [longitud, latitud]
 * (al revés que Leaflet, que usa [lat, lng]).
 */
public record Punto(String type, double[] coordinates) {

    public static Punto de(double longitud, double latitud) {
        return new Punto("Point", new double[]{longitud, latitud});
    }
}

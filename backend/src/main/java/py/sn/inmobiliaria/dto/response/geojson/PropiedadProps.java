package py.sn.inmobiliaria.dto.response.geojson;

import java.math.BigDecimal;
import java.util.List;

/**
 * Propiedades de una propiedad en el mapa y en las tarjetas. Coincide campo por campo con
 * {@code PropiedadProps} de types.ts, más dos campos nuevos que el frontend puede ignorar:
 * {@code operaciones} y {@code descripcion}.
 *
 * @param id            código público (prop-001), no el id interno
 * @param categoria     "Casa" o "Terreno"
 * @param tipoOperacion operación "principal": la que coincide con el filtro o, si no hay filtro, Venta
 * @param precio        precio de la operación principal
 * @param moneda        moneda de la operación principal
 * @param dormitorios   habitaciones (0 en terrenos)
 * @param banos         baños (0 en terrenos)
 * @param m2            superficie construida (casa) o superficie del terreno
 * @param fotos         URLs de las imágenes; la principal primero
 * @param operaciones   todas las operaciones con su precio (una casa puede tener Venta y Alquiler)
 */
public record PropiedadProps(
        String id,
        String titulo,
        String categoria,
        String tipoOperacion,
        String ciudad,
        String barrio,
        int barrioId,
        BigDecimal precio,
        String moneda,
        int dormitorios,
        int banos,
        BigDecimal m2,
        String direccion,
        List<String> fotos,
        List<OperacionDto> operaciones,
        String descripcion) {

    /** Una operación con su precio. */
    public record OperacionDto(String operacion, BigDecimal precio, String moneda) {
    }
}

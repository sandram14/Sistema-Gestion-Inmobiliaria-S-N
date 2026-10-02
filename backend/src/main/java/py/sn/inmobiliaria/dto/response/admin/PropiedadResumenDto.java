package py.sn.inmobiliaria.dto.response.admin;

import py.sn.inmobiliaria.dto.response.geojson.PropiedadProps.OperacionDto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fila del listado del panel administrativo (/admin/propiedades).
 *
 * @param id              id interno (el que usan las rutas /api/admin/propiedades/{id})
 * @param imagenPrincipal URL de la imagen principal, o null si no tiene
 */
public record PropiedadResumenDto(
        long id,
        String codigo,
        String titulo,
        String tipo,
        String estado,
        String ciudad,
        String barrio,
        List<OperacionDto> operaciones,
        String imagenPrincipal,
        OffsetDateTime actualizadoEn) {
}

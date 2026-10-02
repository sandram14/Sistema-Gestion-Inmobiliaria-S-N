package py.sn.inmobiliaria.dto.response.admin;

import py.sn.inmobiliaria.dto.response.geojson.PropiedadProps.OperacionDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Propiedad completa para el formulario de edición del panel (/admin/propiedades/:id).
 * Sus campos coinciden con los de PropiedadRequest, así el formulario puede cargar
 * estos datos, modificarlos y enviarlos de vuelta.
 *
 * @param casa    datos de casa, o null si es terreno
 * @param terreno datos de terreno, o null si es casa
 */
public record PropiedadDetalleDto(
        long id,
        String codigo,
        String titulo,
        String descripcion,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        String tipo,
        String estado,
        Ubicacion ubicacion,
        Casa casa,
        Terreno terreno,
        List<OperacionDto> operaciones,
        List<ImagenDto> imagenes,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn) {

    /** Barrio, ciudad y departamento (para preseleccionar los combos del formulario). */
    public record Ubicacion(int idBarrio, String barrio, int idCiudad, String ciudad,
                            int idDepartamento, String departamento) {
    }

    public record Casa(int habitaciones, int banos, boolean sala, boolean comedor, boolean cocina,
                       BigDecimal superficieConstruida, BigDecimal superficieTerreno) {
    }

    public record Terreno(BigDecimal superficie, BigDecimal frente, BigDecimal fondo, boolean tieneTitulo) {
    }

    public record ImagenDto(long id, String url, String descripcion, boolean esPrincipal, int orden) {
    }
}

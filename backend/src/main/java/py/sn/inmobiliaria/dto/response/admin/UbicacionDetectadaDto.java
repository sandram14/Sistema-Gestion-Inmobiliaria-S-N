package py.sn.inmobiliaria.dto.response.admin;

import java.math.BigDecimal;

/**
 * Respuesta de GET /api/admin/ubicacion: el punto marcado y dónde está.
 *
 * <pre>
 * { "latitud": -25.511700, "longitud": -54.611800,
 *   "departamento": {"id": 1, "nombre": "ALTO PARANÁ"},
 *   "ciudad":       {"id": 1, "nombre": "CIUDAD DEL ESTE"},
 *   "barrio":       {"id": 10011013, "nombre": "AREA 1"} }
 * </pre>
 *
 * <p>Las coordenadas vienen redondeadas a 6 decimales, la precisión con la que se guardan;
 * el formulario puede enviarlas tal cual al crear o editar la propiedad. Los ids coinciden
 * con los de GET /api/catalogos y con el campo idBarrio de PropiedadRequest.</p>
 */
public record UbicacionDetectadaDto(
        BigDecimal latitud,
        BigDecimal longitud,
        Elemento departamento,
        Elemento ciudad,
        Elemento barrio) {

    public record Elemento(int id, String nombre) {
    }
}

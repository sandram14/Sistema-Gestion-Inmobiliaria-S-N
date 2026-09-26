package py.sn.inmobiliaria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Formato único de error de la API.
 *
 * <pre>
 * { "error": { "codigo": "NO_ENCONTRADO", "mensaje": "...", "campos": { ... } } }
 * </pre>
 *
 * <p>Nunca incluye stack traces ni detalles internos.</p>
 */
public record ErrorResponse(Detalle error) {

    public static ErrorResponse de(String codigo, String mensaje) {
        return new ErrorResponse(new Detalle(codigo, mensaje, Map.of()));
    }

    public static ErrorResponse de(String codigo, String mensaje, Map<String, String> campos) {
        return new ErrorResponse(new Detalle(codigo, mensaje, campos == null ? Map.of() : Map.copyOf(campos)));
    }

    /**
     * @param codigo  código estable, pensado para el frontend (VALIDACION, NO_ENCONTRADO, ...)
     * @param mensaje texto legible para el usuario
     * @param campos  errores por campo; se omite del JSON si está vacío
     */
    public record Detalle(
            String codigo,
            String mensaje,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> campos) {
    }
}

package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Los datos recibidos no son válidos (HTTP 400). Incluye el error de cada campo,
 * para que el formulario del frontend pueda mostrarlo junto al campo correspondiente.
 */
public class ValidacionException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ValidacionException(Map<String, String> campos) {
        super(HttpServletResponse.SC_BAD_REQUEST, "VALIDACION", "Hay datos inválidos. Revise los campos indicados.", campos);
    }

    public static ValidacionException deCampo(String campo, String mensaje) {
        return new ValidacionException(Map.of(campo, mensaje));
    }
}

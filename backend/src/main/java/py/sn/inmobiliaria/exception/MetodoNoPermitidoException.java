package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/** El recurso existe pero no admite el método HTTP usado (HTTP 405). */
public class MetodoNoPermitidoException extends ApiException {

    private static final long serialVersionUID = 1L;

    public MetodoNoPermitidoException(String metodo) {
        super(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "El método " + metodo + " no está permitido para este recurso");
    }
}

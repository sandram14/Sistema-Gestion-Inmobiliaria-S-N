package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/** El recurso solicitado no existe (HTTP 404). */
public class NoEncontradoException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String mensaje) {
        super(HttpServletResponse.SC_NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }
}

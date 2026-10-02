package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/** El dato choca con uno existente, por ejemplo un código o email repetido (HTTP 409). */
public class ConflictoException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String mensaje) {
        super(HttpServletResponse.SC_CONFLICT, "CONFLICTO", mensaje);
    }
}

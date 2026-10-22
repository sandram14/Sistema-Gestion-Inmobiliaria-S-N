package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/** Hay sesión, pero la petición no está permitida (HTTP 403). */
public class AccesoDenegadoException extends ApiException {

    private static final long serialVersionUID = 1L;

    public AccesoDenegadoException(String mensaje) {
        super(HttpServletResponse.SC_FORBIDDEN, "ACCESO_DENEGADO", mensaje);
    }
}

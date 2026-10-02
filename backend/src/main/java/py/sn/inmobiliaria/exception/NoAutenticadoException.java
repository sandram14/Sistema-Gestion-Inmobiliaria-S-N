package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/** Falta iniciar sesión, la sesión expiró o las credenciales son incorrectas (HTTP 401). */
public class NoAutenticadoException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NoAutenticadoException(String mensaje) {
        super(HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", mensaje);
    }
}

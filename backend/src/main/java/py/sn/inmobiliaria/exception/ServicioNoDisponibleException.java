package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/** Un recurso necesario (por ejemplo, la base de datos) no responde (HTTP 503). */
public class ServicioNoDisponibleException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", mensaje, null, causa);
    }
}

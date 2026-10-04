package py.sn.inmobiliaria.exception;

/** El visitante superó el límite de consultas al asistente (HTTP 429). */
public class DemasiadasConsultasException extends ApiException {

    private static final long serialVersionUID = 1L;

    public DemasiadasConsultasException(String mensaje) {
        super(429, "DEMASIADAS_CONSULTAS", mensaje);
    }
}

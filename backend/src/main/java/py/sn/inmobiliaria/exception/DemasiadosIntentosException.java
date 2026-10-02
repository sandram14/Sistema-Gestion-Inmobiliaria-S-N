package py.sn.inmobiliaria.exception;

/** Demasiados intentos fallidos de login (HTTP 429 Too Many Requests). */
public class DemasiadosIntentosException extends ApiException {

    private static final long serialVersionUID = 1L;

    public DemasiadosIntentosException(long minutos) {
        super(429, "DEMASIADOS_INTENTOS",
                "Demasiados intentos fallidos. Espere " + minutos + (minutos == 1 ? " minuto" : " minutos")
                        + " antes de volver a intentar.");
    }
}

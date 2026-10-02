package py.sn.inmobiliaria.exception;

/**
 * La operación viola una regla de negocio (HTTP 422 Unprocessable Content):
 * los datos tienen el formato correcto, pero lo que se pide no está permitido.
 */
public class ReglaNegocioException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(422, codigo, mensaje);
    }
}

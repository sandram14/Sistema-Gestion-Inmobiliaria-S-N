package py.sn.inmobiliaria.exception;

import java.util.Map;

/**
 * Excepción base de la API. Lleva el código HTTP y el código de error que
 * verá el frontend. Las subclases representan situaciones concretas.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int estadoHttp;
    private final String codigo;
    private final transient Map<String, String> campos;

    public ApiException(int estadoHttp, String codigo, String mensaje) {
        this(estadoHttp, codigo, mensaje, Map.of());
    }

    public ApiException(int estadoHttp, String codigo, String mensaje, Map<String, String> campos) {
        this(estadoHttp, codigo, mensaje, campos, null);
    }

    /** Con causa técnica: queda en el log del servidor, nunca se envía al cliente. */
    public ApiException(int estadoHttp, String codigo, String mensaje, Map<String, String> campos, Throwable causa) {
        super(mensaje, causa);
        this.estadoHttp = estadoHttp;
        this.codigo = codigo;
        this.campos = campos == null ? Map.of() : Map.copyOf(campos);
    }

    public int estadoHttp() {
        return estadoHttp;
    }

    public String codigo() {
        return codigo;
    }

    public Map<String, String> campos() {
        return campos;
    }
}

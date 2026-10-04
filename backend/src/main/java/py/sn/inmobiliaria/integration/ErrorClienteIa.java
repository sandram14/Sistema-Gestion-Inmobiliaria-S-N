package py.sn.inmobiliaria.integration;

/** Falla al consultar el servicio de IA, clasificada para decidir qué responder al visitante. */
public class ErrorClienteIa extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Tipo {
        /** Cuota del nivel gratuito agotada (HTTP 429). */
        CUOTA_AGOTADA,
        /** El servicio no respondió a tiempo o tuvo un error temporal. */
        NO_DISPONIBLE,
        /** Clave inválida, modelo inexistente u otro error de configuración (HTTP 400/403/404). */
        CONFIGURACION,
        /** El contenido fue bloqueado por los filtros de seguridad del modelo. */
        BLOQUEADA
    }

    private final Tipo tipo;
    private final boolean reintentable;

    public ErrorClienteIa(Tipo tipo, String mensaje) {
        this(tipo, mensaje, false);
    }

    /** @param reintentable true si es un error temporal del servidor (vale la pena un reintento) */
    public ErrorClienteIa(Tipo tipo, String mensaje, boolean reintentable) {
        super(mensaje);
        this.tipo = tipo;
        this.reintentable = reintentable;
    }

    public ErrorClienteIa(Tipo tipo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.tipo = tipo;
        this.reintentable = false;
    }

    public Tipo tipo() {
        return tipo;
    }

    public boolean reintentable() {
        return reintentable;
    }
}

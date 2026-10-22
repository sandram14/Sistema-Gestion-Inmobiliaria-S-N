package py.sn.inmobiliaria.dto.request;

/** Peticiones de la cuenta propia. */
public final class CuentaRequest {

    private CuentaRequest() {
    }

    /** Datos personales editables por el propio usuario (el email y el rol no se cambian aquí). */
    public record Datos(String nombre, String apellido, String cedula, String telefono) {
    }

    public record CambioEmail(String email, String passwordActual) {
    }

    public record CambioContrasena(String passwordActual, String passwordNueva) {
    }

    public record Estado(Boolean activo) {
    }
}

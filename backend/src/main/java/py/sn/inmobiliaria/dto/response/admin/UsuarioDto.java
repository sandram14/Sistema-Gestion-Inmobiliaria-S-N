package py.sn.inmobiliaria.dto.response.admin;

import java.time.OffsetDateTime;

/** Usuario del panel, sin datos sensibles (nunca incluye la contraseña ni su hash). */
public record UsuarioDto(int id, String nombre, String apellido, String email, String cedula, String telefono,
                         String rol, boolean activo, boolean debeCambiarPassword, OffsetDateTime creadoEn,
                         long propiedadesAsignadas) {

    /** Respuesta del alta o del restablecimiento: la contraseña temporal se muestra UNA sola vez. */
    public record ConContrasenaTemporal(UsuarioDto usuario, String contrasenaTemporal) {
    }
}

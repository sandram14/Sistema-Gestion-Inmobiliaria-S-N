package py.sn.inmobiliaria.dto.request;

/** Alta o edición de un usuario por el administrador. El rol es ADMIN, GERENTE o VENDEDOR. */
public record UsuarioRequest(String nombre, String apellido, String email, String cedula, String telefono, String rol) {
}

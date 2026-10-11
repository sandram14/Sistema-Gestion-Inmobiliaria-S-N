package py.sn.inmobiliaria.seguridad;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Usuario autenticado, tal como se guarda en la sesión y se devuelve en /me.
 *
 * <p>El backend lo vuelve a leer de la base en cada petición administrativa (AuthFilter):
 * si el usuario fue desactivado o cambió de rol, el efecto es inmediato.</p>
 *
 * @param permisos            permisos del rol, ordenados (el panel los usa para mostrar opciones)
 * @param debeCambiarPassword true si tiene una contraseña temporal que debe cambiar
 */
public record UsuarioSesion(int id, String nombre, String apellido, String email, String rol,
                            List<String> permisos, boolean debeCambiarPassword) implements Serializable {

    private static final long serialVersionUID = 2L;

    public static final String ATRIBUTO = "sn.usuario";
    /** Atributo de la petición con el usuario recién releído de la base. */
    public static final String ATRIBUTO_PETICION = "sn.usuarioActual";
    public static final String ROL_ADMIN = Permiso.ADMIN;

    /** Construye la sesión con los permisos que corresponden al rol. */
    public UsuarioSesion(int id, String nombre, String apellido, String email, String rol, boolean debeCambiarPassword) {
        this(id, nombre, apellido, email, rol,
                Permiso.deRol(rol).stream().map(Enum::name).sorted().toList(), debeCambiarPassword);
    }

    /** Sin contraseña temporal. */
    public UsuarioSesion(int id, String nombre, String apellido, String email, String rol) {
        this(id, nombre, apellido, email, rol, false);
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    public boolean tiene(Permiso permiso) {
        return permisos.contains(permiso.name());
    }

    public Set<Permiso> conjuntoDePermisos() {
        return Permiso.deRol(rol);
    }
}

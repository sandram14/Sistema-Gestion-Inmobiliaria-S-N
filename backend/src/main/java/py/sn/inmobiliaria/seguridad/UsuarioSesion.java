package py.sn.inmobiliaria.seguridad;

import java.io.Serializable;

/**
 * Datos del administrador guardados en la sesión HTTP después del login.
 * Nunca incluye la contraseña ni su hash. Es Serializable porque Tomcat puede guardar
 * las sesiones en disco al reiniciarse.
 */
public record UsuarioSesion(int id, String nombre, String apellido, String email, String rol)
        implements Serializable {

    /** Nombre del atributo en la HttpSession. */
    public static final String ATRIBUTO = "sn.usuario";

    public static final String ROL_ADMIN = "ADMIN";

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }
}

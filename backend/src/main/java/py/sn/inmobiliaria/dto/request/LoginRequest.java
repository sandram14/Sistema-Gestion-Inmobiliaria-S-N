package py.sn.inmobiliaria.dto.request;

/**
 * Cuerpo de POST /api/admin/auth/login.
 *
 * <pre>{ "email": "admin@sn-inmobiliaria.local", "password": "..." }</pre>
 */
public record LoginRequest(String email, String password) {

    /** Evita que la contraseña aparezca en logs si alguien imprime el objeto. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}

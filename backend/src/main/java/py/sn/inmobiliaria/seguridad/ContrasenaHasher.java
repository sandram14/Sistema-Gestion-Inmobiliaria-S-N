package py.sn.inmobiliaria.seguridad;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hash de contraseñas con BCrypt.
 *
 * <p>BCrypt es deliberadamente lento (factor de costo 12 ≈ un cuarto de segundo por intento):
 * para quien inicia sesión es imperceptible, pero para alguien que robara la base y quisiera
 * probar millones de contraseñas sería inviable. Además, cada hash incluye su propia "sal"
 * aleatoria: dos usuarios con la misma contraseña tienen hashes distintos.</p>
 *
 * <p>Compatible con los hashes $2a$ que genera pgcrypto en database/seed/04_admin_dev.sh.</p>
 */
public class ContrasenaHasher {

    public static final int COSTO = 12;

    /**
     * Hash válido de una contraseña que nadie conoce. Se usa para comparar cuando el email
     * no existe, así la respuesta tarda lo mismo y no revela qué emails están registrados.
     */
    private static final String HASH_FICTICIO = BCrypt.hashpw("contraseña-inexistente", BCrypt.gensalt(COSTO));

    private final int costo;

    public ContrasenaHasher() {
        this(COSTO);
    }

    /** Permite un costo menor en las pruebas automáticas, para que sean rápidas. */
    public ContrasenaHasher(int costo) {
        this.costo = costo;
    }

    public String hashear(String contrasena) {
        return BCrypt.hashpw(contrasena, BCrypt.gensalt(costo));
    }

    /** true si la contraseña corresponde al hash. Nunca lanza excepción por un hash mal formado. */
    public boolean verificar(String contrasena, String hash) {
        if (contrasena == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(contrasena, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Hace el mismo trabajo que una verificación real, con un hash que nunca coincide. */
    public void verificarFicticio(String contrasena) {
        verificar(contrasena == null ? "" : contrasena, HASH_FICTICIO);
    }
}

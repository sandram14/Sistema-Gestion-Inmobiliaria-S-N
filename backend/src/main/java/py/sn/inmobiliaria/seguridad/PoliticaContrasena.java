package py.sn.inmobiliaria.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Reglas de contraseñas del panel: mínimo 10 caracteres, con al menos una letra y un número.
 * BCrypt solo usa los primeros 72 bytes, por eso también hay un máximo.
 */
public final class PoliticaContrasena {

    public static final int MINIMO = 10;
    public static final int MAXIMO_BYTES = 72;
    // Sin caracteres que se confunden al leerlos (0/O, 1/l/I)
    private static final String LETRAS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String NUMEROS = "23456789";
    private static final SecureRandom AZAR = new SecureRandom();

    private PoliticaContrasena() {
    }

    /** @return el problema de la contraseña, o null si cumple las reglas */
    public static String problema(String contrasena) {
        if (contrasena == null || contrasena.length() < MINIMO) {
            return "Debe tener al menos " + MINIMO + " caracteres";
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            return "Es demasiado larga (máximo " + MAXIMO_BYTES + " bytes)";
        }
        if (contrasena.chars().noneMatch(Character::isLetter) || contrasena.chars().noneMatch(Character::isDigit)) {
            return "Debe incluir al menos una letra y un número";
        }
        return null;
    }

    /** Contraseña temporal aleatoria de 12 caracteres que cumple las reglas. */
    public static String generarTemporal() {
        String todos = LETRAS + NUMEROS;
        char[] c = new char[12];
        c[0] = LETRAS.charAt(AZAR.nextInt(LETRAS.length()));
        c[1] = NUMEROS.charAt(AZAR.nextInt(NUMEROS.length()));
        for (int i = 2; i < c.length; i++) {
            c[i] = todos.charAt(AZAR.nextInt(todos.length()));
        }
        for (int i = c.length - 1; i > 0; i--) { // mezcla para que la letra y el número no queden siempre al inicio
            int j = AZAR.nextInt(i + 1);
            char t = c[i];
            c[i] = c[j];
            c[j] = t;
        }
        return new String(c);
    }
}

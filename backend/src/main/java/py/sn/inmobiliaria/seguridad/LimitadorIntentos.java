package py.sn.inmobiliaria.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de login para frenar a quien prueba contraseñas al azar.
 *
 * <p>Regla: como máximo {@code maxFallos} fallos por combinación IP + email dentro de una
 * ventana de tiempo. Funciona como una puerta que se traba unos minutos después de varios
 * intentos con la llave equivocada. Un login correcto borra el historial.</p>
 *
 * <p>Se guarda en memoria: alcanza para un único servidor con pocos administradores.</p>
 */
public class LimitadorIntentos {

    private final int maxFallos;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public LimitadorIntentos() {
        this(5, Duration.ofMinutes(15), Clock.systemUTC());
    }

    public LimitadorIntentos(int maxFallos, Duration ventana, Clock reloj) {
        this.maxFallos = maxFallos;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    /** Minutos que faltan para poder reintentar, o 0 si puede intentar ahora. */
    public long minutosDeBloqueo(String ip, String email) {
        Deque<Instant> lista = fallos.get(clave(ip, email));
        if (lista == null) {
            return 0;
        }
        synchronized (lista) {
            limpiar(lista);
            if (lista.size() < maxFallos) {
                return 0;
            }
            Instant desbloqueo = lista.peekFirst().plus(ventana);
            long segundos = Duration.between(reloj.instant(), desbloqueo).getSeconds();
            return Math.max(1, (segundos + 59) / 60);
        }
    }

    public void registrarFallo(String ip, String email) {
        Deque<Instant> lista = fallos.computeIfAbsent(clave(ip, email), k -> new ArrayDeque<>());
        synchronized (lista) {
            limpiar(lista);
            lista.addLast(reloj.instant());
        }
    }

    public void registrarExito(String ip, String email) {
        fallos.remove(clave(ip, email));
    }

    private void limpiar(Deque<Instant> lista) {
        Instant limite = reloj.instant().minus(ventana);
        while (!lista.isEmpty() && lista.peekFirst().isBefore(limite)) {
            lista.pollFirst();
        }
    }

    private static String clave(String ip, String email) {
        return (ip == null ? "?" : ip) + "|" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }
}

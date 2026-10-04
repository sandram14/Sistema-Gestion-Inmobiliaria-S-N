package py.sn.inmobiliaria.service.asistente;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límites de uso del asistente, para no agotar la cuota gratuita de Gemini.
 *
 * <p>Funciona como un contador de turnos con dos ventanas (último minuto y últimas 24 horas),
 * por visitante y para todo el sitio. Se guarda en memoria: alcanza para un único servidor.</p>
 */
public class LimitadorAsistente {

    /** Resultado de pedir permiso para una consulta. */
    public enum Resultado { PERMITIDA, LIMITE_SESION, LIMITE_GLOBAL }

    private static final Duration MINUTO = Duration.ofMinutes(1);
    private static final Duration DIA = Duration.ofDays(1);
    private static final int MAX_SESIONES_EN_MEMORIA = 10_000;

    private final int minutoSesion;
    private final int diaSesion;
    private final int minutoGlobal;
    private final int diaGlobal;
    private final Clock reloj;

    private final Map<UUID, Deque<Instant>> porSesion = new ConcurrentHashMap<>();
    private final Deque<Instant> global = new ArrayDeque<>();

    public LimitadorAsistente(int minutoSesion, int diaSesion, int minutoGlobal, int diaGlobal, Clock reloj) {
        this.minutoSesion = minutoSesion;
        this.diaSesion = diaSesion;
        this.minutoGlobal = minutoGlobal;
        this.diaGlobal = diaGlobal;
        this.reloj = reloj;
    }

    /**
     * Si la consulta está permitida, la registra (consume un turno). Si no, no registra nada.
     */
    public synchronized Resultado intentar(UUID sesion) {
        Instant ahora = reloj.instant();
        limpiar(global, ahora);
        Deque<Instant> deSesion = porSesion.computeIfAbsent(sesion, s -> new ArrayDeque<>());
        limpiar(deSesion, ahora);

        if (contarDesde(deSesion, ahora.minus(MINUTO)) >= minutoSesion || deSesion.size() >= diaSesion) {
            return Resultado.LIMITE_SESION;
        }
        if (contarDesde(global, ahora.minus(MINUTO)) >= minutoGlobal || global.size() >= diaGlobal) {
            return Resultado.LIMITE_GLOBAL;
        }
        deSesion.addLast(ahora);
        global.addLast(ahora);
        if (porSesion.size() > MAX_SESIONES_EN_MEMORIA) {
            porSesion.values().removeIf(d -> {
                limpiar(d, ahora);
                return d.isEmpty();
            });
        }
        return Resultado.PERMITIDA;
    }

    /** Descarta los registros de más de 24 horas. */
    private static void limpiar(Deque<Instant> registros, Instant ahora) {
        Instant limite = ahora.minus(DIA);
        while (!registros.isEmpty() && !registros.peekFirst().isAfter(limite)) {
            registros.pollFirst();
        }
    }

    private static long contarDesde(Deque<Instant> registros, Instant desde) {
        return registros.stream().filter(i -> i.isAfter(desde)).count();
    }
}

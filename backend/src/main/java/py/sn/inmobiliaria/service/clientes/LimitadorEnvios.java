package py.sn.inmobiliaria.service.clientes;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Límite simple de envíos por clave en una ventana de tiempo (en memoria; se reinicia con el servidor). */
public class LimitadorEnvios {

    private final int maximo;
    private final long ventanaMs;
    private final Clock reloj;
    private final Map<String, Deque<Long>> envios = new ConcurrentHashMap<>();

    public LimitadorEnvios(int maximo, long ventanaMs, Clock reloj) {
        this.maximo = maximo;
        this.ventanaMs = ventanaMs;
        this.reloj = reloj;
    }

    /** @return true si se permite el envío (y lo cuenta); false si se superó el límite. */
    public boolean permitir(String clave) {
        long ahora = reloj.millis();
        Deque<Long> d = envios.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (d) {
            while (!d.isEmpty() && d.peekFirst() <= ahora - ventanaMs) {
                d.pollFirst();
            }
            if (d.size() >= maximo) {
                return false;
            }
            d.addLast(ahora);
            return true;
        }
    }
}

package py.sn.inmobiliaria.seguridad;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LimitadorIntentosTest {

    /** Reloj que se puede adelantar manualmente. */
    static final class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-10-02T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private final RelojManual reloj = new RelojManual();
    private final LimitadorIntentos limitador = new LimitadorIntentos(5, Duration.ofMinutes(15), reloj);

    private void fallar(int veces, String email) {
        for (int i = 0; i < veces; i++) {
            limitador.registrarFallo("10.0.0.1", email);
        }
    }

    @Test
    void bloqueaDespuesDeCincoFallos() {
        fallar(4, "admin@x.com");
        assertEquals(0, limitador.minutosDeBloqueo("10.0.0.1", "admin@x.com"));

        fallar(1, "admin@x.com");
        assertEquals(15, limitador.minutosDeBloqueo("10.0.0.1", "ADMIN@x.com")); // sin distinguir mayúsculas
    }

    @Test
    void elBloqueoEsPorIpYEmail() {
        fallar(5, "admin@x.com");

        assertEquals(0, limitador.minutosDeBloqueo("10.0.0.2", "admin@x.com"));
        assertEquals(0, limitador.minutosDeBloqueo("10.0.0.1", "otro@x.com"));
    }

    @Test
    void seDesbloqueaAlPasarLaVentana() {
        fallar(5, "admin@x.com");

        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(10));
        assertEquals(5, limitador.minutosDeBloqueo("10.0.0.1", "admin@x.com"));

        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(6));
        assertEquals(0, limitador.minutosDeBloqueo("10.0.0.1", "admin@x.com"));
    }

    @Test
    void unLoginCorrectoBorraLosFallos() {
        fallar(4, "admin@x.com");
        limitador.registrarExito("10.0.0.1", "admin@x.com");
        fallar(4, "admin@x.com");

        assertEquals(0, limitador.minutosDeBloqueo("10.0.0.1", "admin@x.com"));
    }
}

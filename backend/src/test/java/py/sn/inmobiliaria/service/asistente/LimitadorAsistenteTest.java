package py.sn.inmobiliaria.service.asistente;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.seguridad.LimitadorIntentosTest;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static py.sn.inmobiliaria.service.asistente.LimitadorAsistente.Resultado.LIMITE_GLOBAL;
import static py.sn.inmobiliaria.service.asistente.LimitadorAsistente.Resultado.LIMITE_SESION;
import static py.sn.inmobiliaria.service.asistente.LimitadorAsistente.Resultado.PERMITIDA;

class LimitadorAsistenteTest {

    private final LimitadorIntentosTest.RelojManual reloj = new LimitadorIntentosTest.RelojManual();

    @Test
    void limitePorMinutoDeCadaVisitante() {
        LimitadorAsistente l = new LimitadorAsistente(3, 100, 100, 1000, reloj);
        UUID ana = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            assertEquals(PERMITIDA, l.intentar(ana));
        }
        assertEquals(LIMITE_SESION, l.intentar(ana));
        assertEquals(PERMITIDA, l.intentar(UUID.randomUUID())); // otro visitante no se ve afectado

        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(61));
        assertEquals(PERMITIDA, l.intentar(ana));
    }

    @Test
    void limitePorDiaDeCadaVisitante() {
        LimitadorAsistente l = new LimitadorAsistente(100, 2, 100, 1000, reloj);
        UUID ana = UUID.randomUUID();

        l.intentar(ana);
        reloj.ahora = reloj.ahora.plus(Duration.ofHours(1));
        l.intentar(ana);
        reloj.ahora = reloj.ahora.plus(Duration.ofHours(1));
        assertEquals(LIMITE_SESION, l.intentar(ana));

        reloj.ahora = reloj.ahora.plus(Duration.ofHours(23));
        assertEquals(PERMITIDA, l.intentar(ana));
    }

    @Test
    void limiteGlobalParaNoSuperarLaCuotaGratuita() {
        LimitadorAsistente l = new LimitadorAsistente(100, 100, 2, 1000, reloj);

        assertEquals(PERMITIDA, l.intentar(UUID.randomUUID()));
        assertEquals(PERMITIDA, l.intentar(UUID.randomUUID()));
        assertEquals(LIMITE_GLOBAL, l.intentar(UUID.randomUUID()));
    }

    @Test
    void unaConsultaRechazadaNoConsumeTurno() {
        LimitadorAsistente l = new LimitadorAsistente(1, 100, 1, 1000, reloj);
        UUID ana = UUID.randomUUID();

        assertEquals(PERMITIDA, l.intentar(ana));
        assertEquals(LIMITE_SESION, l.intentar(ana));
        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(61));
        assertEquals(PERMITIDA, l.intentar(UUID.randomUUID())); // el rechazo anterior no sumó al global
    }
}

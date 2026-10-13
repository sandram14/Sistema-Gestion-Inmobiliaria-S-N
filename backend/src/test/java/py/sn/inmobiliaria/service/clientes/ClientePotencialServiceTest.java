package py.sn.inmobiliaria.service.clientes;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.ClienteDtos;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.validacion.Errores;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientePotencialServiceTest {

    private final ClientePotencialService servicio = new ClientePotencialService(null); // las validaciones fallan antes de la base

    private static Map<String, String> errores(Runnable r) {
        return assertThrows(ValidacionException.class, r::run).campos();
    }

    @Test
    void exigeNombreYAlMenosUnMedioDeContactoValido() {
        Errores e = new Errores();
        ClientePotencialService.validar("A", null, null, null, e);
        var ex = assertThrows(ValidacionException.class, e::lanzarSiHay);
        assertTrue(ex.campos().containsKey("nombre"));
        assertTrue(ex.campos().containsKey("contacto"));
        assertTrue(errores(() -> { Errores x = new Errores(); ClientePotencialService.validar("Ana", "abc", "no-email", null, x); x.lanzarSiHay(); })
                .keySet().containsAll(java.util.Set.of("telefono", "email")));
        Errores ok = new Errores();
        var d = ClientePotencialService.validar("  Ana   Pérez ", "+595 981 123-456", null, "Casa con patio", ok);
        assertDoesNotThrow(ok::lanzarSiHay);
        assertEquals("Ana Pérez", d.nombre());
    }

    @Test
    void desdeElChatEsObligatorioAceptarElTratamientoConLaVersionVigente() {
        var sinAceptar = new ClienteDtos.Solicitud("Ana", "0981123456", null, null, null, false, ClientePotencialService.VERSION_CONSENTIMIENTO);
        assertTrue(errores(() -> servicio.registrarDesdeChat(sinAceptar, null)).containsKey("aceptaTratamiento"));
        var versionVieja = new ClienteDtos.Solicitud("Ana", "0981123456", null, null, null, true, "2020-01");
        assertTrue(errores(() -> servicio.registrarDesdeChat(versionVieja, null)).containsKey("versionConsentimiento"));
    }

    @Test
    void elAltaManualExigeUnMedioValido() {
        var vendedor = new UsuarioSesion(3, "V", "B", "v@b.c", "VENDEDOR");
        var chat = new ClienteDtos.Manual("Ana", "0981123456", null, null, null, "CHAT", null);
        assertTrue(errores(() -> servicio.registrarManual(chat, vendedor)).containsKey("origen")); // el chat no se registra a mano
    }

    @Test
    void estadosValidosYAlcancePorRol() {
        assertEquals("CONTACTADO", ClientePotencialService.opcionEn("contactado", ClientePotencialService.ESTADOS, "estado"));
        assertThrows(ValidacionException.class, () -> ClientePotencialService.opcionEn("INTERESADO", ClientePotencialService.ESTADOS, "estado"));
        assertNull(ClientePotencialService.alcance(new UsuarioSesion(1, "A", "B", "a@b.c", "GERENTE")));
        assertEquals(3, ClientePotencialService.alcance(new UsuarioSesion(3, "V", "B", "v@b.c", "VENDEDOR")));
    }

    @Test
    void elResponsablePorDefectoEsElDeLaPropiedadSiEstaActivo() {
        Usuario activo = new Usuario("V", "B", "v@b.c", "$2a$12$" + "a".repeat(53), null);
        Propiedad p = Entidades.crear(Propiedad.class, Map.of("codigo", "prop-001"));
        assertNull(ClientePotencialService.responsableDe(null));
        assertNull(ClientePotencialService.responsableDe(p));       // sin responsable: sin asignar
        p.setResponsable(activo);
        assertSame(activo, ClientePotencialService.responsableDe(p));
        activo.setActivo(false);
        assertNull(ClientePotencialService.responsableDe(p));       // desactivado: sin asignar
    }

    @Test
    void elLimitadorCuentaPorVentanaDeTiempo() {
        var reloj = new Clock() {
            Instant ahora = Instant.parse("2026-10-13T10:00:00Z");
            public ZoneOffset getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return ahora; }
        };
        var l = new LimitadorEnvios(3, 3_600_000L, reloj);
        assertTrue(l.permitir("a"));
        assertTrue(l.permitir("a"));
        assertTrue(l.permitir("a"));
        assertFalse(l.permitir("a"));
        assertTrue(l.permitir("b"));                                // otra clave, otro contador
        reloj.ahora = reloj.ahora.plusSeconds(3601);
        assertTrue(l.permitir("a"));                                // pasada la hora, vuelve a permitir
    }
}

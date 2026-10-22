package py.sn.inmobiliaria.service.consultas;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaAdminServiceTest {

    @Test
    void alcanceSegunElRol() {
        assertNull(ConsultaAdminService.alcance(new UsuarioSesion(1, "A", "B", "a@b.c", "ADMIN")));
        assertNull(ConsultaAdminService.alcance(new UsuarioSesion(2, "G", "B", "g@b.c", "GERENTE")));
        assertEquals(3, ConsultaAdminService.alcance(new UsuarioSesion(3, "V", "B", "v@b.c", "VENDEDOR")));
    }

    @Test
    void elResumenRecortaLasPreguntasLargas() {
        assertEquals(160, ConsultaAdminService.resumir("x".repeat(500)).length());
        assertEquals("Hola mundo", ConsultaAdminService.resumir("  Hola\n  mundo "));
    }

    @Test
    void filtrosYEstadoInvalidosSeRechazan() {
        var s = new ConsultaAdminService(null, null, java.util.List::of);
        var vendedor = new UsuarioSesion(3, "V", "B", "v@b.c", "VENDEDOR");
        assertThrows(ValidacionException.class, () -> s.historial(null, null, null, "ATENDIDA", null, 1, vendedor));
        assertThrows(ValidacionException.class, () -> s.historial(null, null, null, null, "INVENTADO", 1, vendedor));
        assertThrows(ValidacionException.class, () -> s.historial(null, null, null, null, null, 0, vendedor));
        assertThrows(ValidacionException.class, () -> s.revisar(1, null, vendedor));
    }
}

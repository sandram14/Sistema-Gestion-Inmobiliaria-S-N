package py.sn.inmobiliaria.service.sitio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.service.sitio.ContenidoSitioService.Contacto;
import py.sn.inmobiliaria.service.sitio.ContenidoSitioService.Inicio;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class ContenidoSitioServiceTest {

    private static final String IMG = "/uploads/sitio-001/0a4c5cd8-14cc-4bbc-a4d8-0f7bc29c541d.png";
    private final ContenidoSitioService servicio = new ContenidoSitioService(null); // las validaciones fallan antes de la base

    private static Map<String, String> errores(Runnable r) {
        return assertThrows(ValidacionException.class, r::run).campos();
    }

    @Test
    void inicio_TituloObligatorio_LongitudesYSoloImagenesDelPanel() {
        var e = errores(() -> ContenidoSitioService.validarInicio(new Inicio("https://otro-sitio.com/a.jpg", null, " ", "x".repeat(301), null, null, null)));
        assertTrue(e.keySet().containsAll(List.of("imagenPortada", "titulo", "subtitulo")));
        var ok = ContenidoSitioService.validarInicio(new Inicio(IMG, "", "  Bienvenidos  ", null, null, null, ""));
        assertEquals("Bienvenidos", ok.titulo());
        assertNull(ok.nosotrosImagen());                         // vacío: se usa la imagen del diseño
    }

    @Test
    void contacto_FormatosYWhatsappInternacionalNormalizado() {
        var e = errores(() -> ContenidoSitioService.validarContacto(new Contacto("", null, null, null, "no-email", "abc", "0981-12", null)));
        assertTrue(e.keySet().containsAll(List.of("nombreEmpresa", "email", "telefono", "whatsapp")));
        var ok = ContenidoSitioService.validarContacto(new Contacto("S&N Inmobiliaria", "", "Av. 1", "Ciudad del Este",
                "info@sn.com.py", "+595 61 500 000", "+595 981 123-456", "Lunes a viernes de 8:00 a 18:00"));
        assertEquals("595981123456", ok.whatsapp());             // solo números, para el enlace wa.me
    }

    @Test
    void detectaLosDatosDeContactoProvisionales() throws Exception {
        var m = new ObjectMapper();
        assertTrue(ContenidoSitioService.esProvisional(m.readTree("{\"email\":\"contacto@sn-inmobiliaria.example\",\"telefono\":\"0981 111 222\",\"whatsapp\":\"595981111222\"}")));
        assertTrue(ContenidoSitioService.esProvisional(m.readTree("{\"email\":\"a@b.com\",\"telefono\":\"+595 000 000 000\",\"whatsapp\":\"595981111222\"}")));
        assertFalse(ContenidoSitioService.esProvisional(m.readTree("{\"email\":\"a@b.com\",\"telefono\":\"0981 111 222\",\"whatsapp\":\"595981111222\"}")));
    }

    @Test
    void seccionesInexistentesOTodaviaNoEditables() {
        assertThrows(NoEncontradoException.class, () -> servicio.leer("precios"));
        assertThrows(NoEncontradoException.class, () -> servicio.guardar("servicios", null, 1)); // entrega B
    }

    @Test
    void destacadas_SinRepetidasYComoMaximo12() {
        assertTrue(errores(() -> servicio.guardarDestacadas(List.of(1L, 2L, 1L))).get("ids").contains("repetidas"));
        assertTrue(errores(() -> servicio.guardarDestacadas(LongStream.rangeClosed(1, 13).boxed().toList())).get("ids").contains("12"));
        assertThrows(ValidacionException.class, () -> servicio.guardarDestacadas(null));
    }
}

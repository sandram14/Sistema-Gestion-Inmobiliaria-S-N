package py.sn.inmobiliaria.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.ValidacionException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlmacenImagenesTest {

    /** Primeros bytes reales de cada formato, seguidos de relleno. */
    public static final byte[] JPEG = conRelleno(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    public static final byte[] PNG = conRelleno(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    public static final byte[] WEBP = conRelleno("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1));

    static byte[] conRelleno(byte[] firma) {
        byte[] b = Arrays.copyOf(firma, 2048);
        Arrays.fill(b, firma.length, b.length, (byte) 7);
        return b;
    }

    @TempDir
    Path carpeta;

    private AlmacenImagenes almacen() {
        return new AlmacenImagenes(carpeta);
    }

    private static ByteArrayInputStream datos(byte[] b) {
        return new ByteArrayInputStream(b);
    }

    @Test
    void guardaLosTresFormatosConNombreGeneradoPorElServidor() throws Exception {
        AlmacenImagenes a = almacen();

        String jpg = a.guardar("prop-017", datos(JPEG));
        String png = a.guardar("prop-017", datos(PNG));
        String webp = a.guardar("terr-013", datos(WEBP));

        assertTrue(jpg.matches("^/uploads/prop-017/[0-9a-f-]{36}\\.jpg$"), jpg);
        assertTrue(png.endsWith(".png"));
        assertTrue(webp.startsWith("/uploads/terr-013/") && webp.endsWith(".webp"));
        assertArrayEquals(JPEG, Files.readAllBytes(a.resolver(jpg).orElseThrow()));
    }

    @Test
    void elTipoSeReconocePorElContenidoNoPorElNombre() {
        AlmacenImagenes a = almacen();
        byte[] ejecutable = conRelleno("MZ\u0090\0".getBytes(StandardCharsets.ISO_8859_1)); // un .exe
        byte[] texto = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThrows(ValidacionException.class, () -> a.guardar("prop-017", datos(ejecutable)));
        assertThrows(ValidacionException.class, () -> a.guardar("prop-017", datos(texto)));
        assertThrows(ValidacionException.class, () -> a.guardar("prop-017", datos(new byte[0])));
    }

    @Test
    void rechazaArchivosDeMasDe5Mb() {
        byte[] grande = Arrays.copyOf(JPEG, (int) AlmacenImagenes.TAMANO_MAXIMO + 1);

        ApiException e = assertThrows(ApiException.class, () -> almacen().guardar("prop-017", datos(grande)));

        assertEquals(413, e.estadoHttp());
    }

    @Test
    void noPermiteEscribirFueraDeLaCarpeta() {
        AlmacenImagenes a = almacen();

        assertThrows(IllegalArgumentException.class, () -> a.guardar("../../etc", datos(JPEG)));
        assertTrue(a.resolver("/uploads/../../etc/passwd").isEmpty());
        assertTrue(a.resolver("/uploads/prop-017/../../x.jpg").isEmpty());
        assertTrue(a.resolver("/images/propiedades/prop-001/1.png").isEmpty());
    }

    @Test
    void eliminaSoloArchivosSubidos() throws Exception {
        AlmacenImagenes a = almacen();
        String url = a.guardar("prop-017", datos(PNG));
        Path archivo = a.resolver(url).orElseThrow();

        a.eliminar("/images/propiedades/prop-001/1.png"); // imagen original del frontend: se ignora
        a.eliminar(null);
        assertTrue(Files.exists(archivo));

        a.eliminar(url);
        assertFalse(Files.exists(archivo));
        a.eliminar(url); // borrar dos veces no falla
    }
}

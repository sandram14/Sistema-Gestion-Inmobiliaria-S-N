package py.sn.inmobiliaria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;
import py.sn.inmobiliaria.exception.ValidacionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda y elimina los archivos de imagen subidos desde el panel.
 *
 * <p>Medidas de seguridad:</p>
 * <ul>
 *   <li>El tipo se reconoce por el <b>contenido</b> del archivo (sus primeros bytes, la "firma"),
 *       no por la extensión ni por lo que declare el navegador: un .exe renombrado a .jpg se rechaza.</li>
 *   <li>Solo JPEG, PNG y WebP, de hasta 5 MB.</li>
 *   <li>El nombre lo genera el servidor (UUID): el nombre original nunca se usa como ruta,
 *       así no se puede escribir fuera de la carpeta (ataques del tipo "../../").</li>
 * </ul>
 *
 * <p>Estructura: {@code UPLOAD_DIR/prop-017/3f2a...c9.jpg}, servida como
 * {@code /uploads/prop-017/3f2a...c9.jpg}.</p>
 */
public class AlmacenImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenImagenes.class);

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    public static final String PREFIJO_URL = "/uploads/";

    private static final Pattern CODIGO = Pattern.compile("^[a-z]+-[0-9]{3,}$");
    /** Ruta pública válida: /uploads/{codigo}/{uuid}.{ext} */
    private static final Pattern RUTA = Pattern.compile(
            "^/uploads/([a-z]+-[0-9]{3,})/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.(jpg|png|webp)$");

    /** Formatos admitidos, con su firma de bytes. */
    public enum Formato {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp");

        public final String extension;
        public final String contentType;

        Formato(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public static Optional<Formato> porExtension(String ext) {
            return Arrays.stream(values()).filter(f -> f.extension.equals(ext)).findFirst();
        }
    }

    private final Path base;

    public AlmacenImagenes(Path base) {
        this.base = base.toAbsolutePath().normalize();
    }

    public Path base() {
        return base;
    }

    /**
     * Valida y guarda la imagen.
     *
     * @return la URL pública relativa (/uploads/prop-017/....jpg)
     */
    public String guardar(String codigoPropiedad, InputStream datos) {
        if (codigoPropiedad == null || !CODIGO.matcher(codigoPropiedad).matches()) {
            throw new IllegalArgumentException("Código de propiedad inválido: " + codigoPropiedad);
        }
        byte[] contenido;
        try {
            contenido = datos.readNBytes((int) TAMANO_MAXIMO + 1);
        } catch (IOException e) {
            throw ValidacionException.deCampo("archivo", "No se pudo leer el archivo enviado");
        }
        if (contenido.length == 0) {
            throw ValidacionException.deCampo("archivo", "El archivo está vacío");
        }
        if (contenido.length > TAMANO_MAXIMO) {
            throw new ApiException(413, "ARCHIVO_DEMASIADO_GRANDE", "La imagen supera el máximo de 5 MB");
        }
        Formato formato = detectar(contenido).orElseThrow(() ->
                ValidacionException.deCampo("archivo", "Formato no admitido. Use imágenes JPEG, PNG o WebP"));

        String nombre = UUID.randomUUID() + "." + formato.extension;
        Path carpeta = base.resolve(codigoPropiedad);
        Path destino = carpeta.resolve(nombre);
        try {
            Files.createDirectories(carpeta);
            // Se escribe primero en un temporal y se mueve: nunca queda un archivo a medio escribir
            Path temporal = Files.createTempFile(carpeta, ".subiendo-", ".tmp");
            Files.write(temporal, contenido);
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ServicioNoDisponibleException("No se pudo guardar la imagen. Intente nuevamente.", e);
        }
        return PREFIJO_URL + codigoPropiedad + "/" + nombre;
    }

    /**
     * Borra el archivo de una URL /uploads/... . Las imágenes originales del frontend
     * (/images/...) no se tocan. Nunca lanza excepción: si no puede borrar, lo registra.
     */
    public void eliminar(String url) {
        resolver(url).ifPresent(archivo -> {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException e) {
                log.warn("No se pudo borrar el archivo {}", archivo, e);
            }
        });
    }

    /**
     * Convierte una URL pública en la ruta del archivo, solo si tiene el formato exacto
     * de las URLs que genera este almacén.
     */
    public Optional<Path> resolver(String url) {
        if (url == null || !RUTA.matcher(url).matches()) {
            return Optional.empty();
        }
        Path archivo = base.resolve(url.substring(PREFIJO_URL.length())).normalize();
        return archivo.startsWith(base) ? Optional.of(archivo) : Optional.empty();
    }

    /** Reconoce el formato por los primeros bytes del contenido. */
    static Optional<Formato> detectar(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(Formato.JPEG);
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of(Formato.PNG);
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(Formato.WEBP);
        }
        return Optional.empty();
    }
}

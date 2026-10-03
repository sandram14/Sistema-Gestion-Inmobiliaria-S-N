package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.service.AlmacenImagenes;
import py.sn.inmobiliaria.service.AlmacenImagenes.Formato;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code GET /uploads/{codigo}/{uuid}.{jpg|png|webp}}: sirve (solo lectura) las imágenes
 * subidas desde el panel. Es un recurso estático, no parte de la API.
 *
 * <p>Solo responde rutas con el formato exacto que genera AlmacenImagenes; cualquier otra
 * (incluidos intentos como "../") es 404. Como el nombre (UUID) nunca se reutiliza, el
 * navegador puede guardar la imagen en caché por mucho tiempo.</p>
 */
@WebServlet(name = "UploadsServlet", urlPatterns = "/uploads/*")
public class UploadsServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = "/uploads" + (req.getPathInfo() == null ? "" : req.getPathInfo());
        AlmacenImagenes almacen = servicio(AlmacenImagenes.class);
        Path archivo = almacen.resolver(url)
                .filter(Files::isRegularFile)
                .orElseThrow(() -> new NoEncontradoException("La imagen no existe"));
        String extension = url.substring(url.lastIndexOf('.') + 1);
        Formato formato = Formato.porExtension(extension)
                .orElseThrow(() -> new NoEncontradoException("La imagen no existe"));

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(formato.contentType);
        resp.setContentLengthLong(Files.size(archivo));
        resp.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        Files.copy(archivo, resp.getOutputStream());
    }
}

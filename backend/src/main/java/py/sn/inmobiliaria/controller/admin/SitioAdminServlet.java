package py.sn.inmobiliaria.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.service.AlmacenImagenes;
import py.sn.inmobiliaria.service.sitio.ContenidoSitioService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestión del sitio web (ADMIN y GERENTE; los permisos los exige AuthFilter según ReglasAcceso).
 * <ul>
 *   <li>GET|PUT /api/admin/sitio/secciones/{inicio|servicios|nosotros|contacto}   (CONTENIDO_EDITAR)</li>
 *   <li>POST    /api/admin/sitio/imagenes   multipart "archivo" → {"url": ...}    (CONTENIDO_EDITAR)</li>
 *   <li>GET|PUT /api/admin/sitio/ofertas    {"ids": [3, 8, 1]} en el orden de la portada (OFERTAS_GESTIONAR)</li>
 * </ul>
 */
@WebServlet(name = "SitioAdminServlet", urlPatterns = "/api/admin/sitio/*")
@MultipartConfig(maxFileSize = AlmacenImagenes.TAMANO_MAXIMO, maxRequestSize = AlmacenImagenes.TAMANO_MAXIMO + 64 * 1024)
public class SitioAdminServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern SECCION = Pattern.compile("^/secciones/([a-z]+)$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = ruta(req);
        if ("/ofertas".equals(ruta)) {
            escribirJson(resp, HttpServletResponse.SC_OK, sitio().destacadas());
            return;
        }
        escribirJson(resp, HttpServletResponse.SC_OK, sitio().leer(seccion(ruta)));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = ruta(req);
        if ("/ofertas".equals(ruta)) {
            Map<?, ?> cuerpo = leerJson(req, Map.class);
            Object ids = cuerpo.get("ids");
            if (!(ids instanceof List<?> lista)) {
                throw ValidacionException.deCampo("ids", "Indique la lista de propiedades");
            }
            List<Long> valores = new ArrayList<>();
            for (Object o : lista) {
                if (!(o instanceof Number n)) throw ValidacionException.deCampo("ids", "Cada elemento debe ser un número");
                valores.add(n.longValue());
            }
            escribirJson(resp, HttpServletResponse.SC_OK, sitio().guardarDestacadas(valores));
            return;
        }
        escribirJson(resp, HttpServletResponse.SC_OK,
                sitio().guardar(seccion(ruta), leerJson(req, JsonNode.class), PropiedadAdminServlet.actor(req).id()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (!"/imagenes".equals(ruta(req))) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        String tipo = req.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("multipart/form-data")) {
            throw new ApiException(415, "TIPO_NO_SOPORTADO", "La imagen debe enviarse como multipart/form-data");
        }
        Part archivo;
        try {
            archivo = req.getPart("archivo");
        } catch (IllegalStateException e) {
            throw new ApiException(413, "ARCHIVO_DEMASIADO_GRANDE", "La imagen supera el máximo de 5 MB");
        }
        if (archivo == null || archivo.getSize() == 0) {
            throw ValidacionException.deCampo("archivo", "Seleccione una imagen");
        }
        String url;
        try (InputStream datos = archivo.getInputStream()) {
            url = servicio(AlmacenImagenes.class).guardar(ContenidoSitioService.CARPETA_IMAGENES, datos); // valida formato y contenido
        }
        escribirJson(resp, HttpServletResponse.SC_CREATED, Map.of("url", url));
    }

    private static String ruta(HttpServletRequest req) {
        return req.getPathInfo() == null ? "/" : req.getPathInfo();
    }

    private static String seccion(String ruta) {
        Matcher m = SECCION.matcher(ruta);
        if (!m.matches()) throw new NoEncontradoException("Recurso no encontrado");
        return m.group(1);
    }

    private ContenidoSitioService sitio() {
        return servicio(ContenidoSitioService.class);
    }
}

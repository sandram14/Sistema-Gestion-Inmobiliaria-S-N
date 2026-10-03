package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.dto.request.ImagenRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.dto.response.admin.PropiedadDetalleDto;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.MetodoNoPermitidoException;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.mapper.PropiedadAdminMapper;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.service.AlmacenImagenes;
import py.sn.inmobiliaria.service.PropiedadAdminService;
import py.sn.inmobiliaria.service.PropiedadAdminService.PropiedadGuardada;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CRUD administrativo de propiedades e imágenes. Protegido por AuthFilter (requiere sesión).
 *
 * <pre>
 * GET    /api/admin/propiedades                                → listado (todos los estados)
 * POST   /api/admin/propiedades                                → crear      (201 + detalle)
 * GET    /api/admin/propiedades/{id}                           → detalle
 * PUT    /api/admin/propiedades/{id}                           → actualizar (200 + detalle)
 * DELETE /api/admin/propiedades/{id}                           → borrado lógico (204)
 * POST   /api/admin/propiedades/{id}/imagenes                  → subir imagen, multipart: archivo [+ descripcion]
 * DELETE /api/admin/propiedades/{id}/imagenes/{idImagen}       → eliminar imagen
 * PATCH  /api/admin/propiedades/{id}/imagenes/{idImagen}/principal → marcar como principal
 * </pre>
 *
 * <p>Las operaciones sobre imágenes devuelven el detalle actualizado de la propiedad, para
 * que el panel pueda refrescar la galería con una sola respuesta.</p>
 */
@WebServlet(name = "PropiedadAdminServlet", urlPatterns = {"/api/admin/propiedades", "/api/admin/propiedades/*"})
@MultipartConfig(maxFileSize = AlmacenImagenes.TAMANO_MAXIMO, maxRequestSize = AlmacenImagenes.TAMANO_MAXIMO + 64 * 1024)
public class PropiedadAdminServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PropiedadAdminServlet.class);

    private static final Pattern PROPIEDAD = Pattern.compile("^/(\\d{1,18})$");
    private static final Pattern IMAGENES = Pattern.compile("^/(\\d{1,18})/imagenes$");
    private static final Pattern IMAGEN = Pattern.compile("^/(\\d{1,18})/imagenes/(\\d{1,18})$");
    private static final Pattern PRINCIPAL = Pattern.compile("^/(\\d{1,18})/imagenes/(\\d{1,18})/principal$");

    // ---- Despacho por método ----

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (esColeccion(req)) {
            escribirJson(resp, HttpServletResponse.SC_OK, admin().listar(PropiedadAdminMapper::resumen));
            return;
        }
        long id = idDe(PROPIEDAD, req);
        escribirJson(resp, HttpServletResponse.SC_OK, detalle(id));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (esColeccion(req)) {
            PropiedadGuardada g = admin().crear(leerJson(req, PropiedadRequest.class));
            resp.setHeader("Location", req.getContextPath() + "/api/admin/propiedades/" + g.id());
            escribirJson(resp, HttpServletResponse.SC_CREATED, detalle(g.id()));
            return;
        }
        long id = idDe(IMAGENES, req);
        subirImagen(id, req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = idDe(PROPIEDAD, req);
        admin().actualizar(id, leerJson(req, PropiedadRequest.class));
        escribirJson(resp, HttpServletResponse.SC_OK, detalle(id));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher imagen = IMAGEN.matcher(ruta(req));
        if (imagen.matches()) {
            long id = Long.parseLong(imagen.group(1));
            String url = admin().eliminarImagen(id, Long.parseLong(imagen.group(2)));
            servicio(AlmacenImagenes.class).eliminar(url); // solo borra archivos de /uploads
            escribirJson(resp, HttpServletResponse.SC_OK, detalle(id));
            return;
        }
        admin().eliminar(idDe(PROPIEDAD, req));
        escribirSinContenido(resp);
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher m = PRINCIPAL.matcher(ruta(req));
        if (!m.matches()) {
            throw rutaInexistenteOMetodo(req);
        }
        long id = Long.parseLong(m.group(1));
        admin().marcarPrincipal(id, Long.parseLong(m.group(2)));
        escribirJson(resp, HttpServletResponse.SC_OK, detalle(id));
    }

    // ---- Imágenes ----

    private void subirImagen(long id, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            throw new ApiException(415, "TIPO_NO_SOPORTADO", "La imagen debe enviarse como multipart/form-data");
        }
        Part archivo;
        try {
            archivo = req.getPart("archivo");
        } catch (IllegalStateException e) {
            // Tomcat rechaza la petición si supera maxFileSize / maxRequestSize
            throw new ApiException(413, "ARCHIVO_DEMASIADO_GRANDE", "La imagen supera el máximo de 5 MB");
        }
        if (archivo == null || archivo.getSize() == 0) {
            throw ValidacionException.deCampo("archivo", "Seleccione una imagen");
        }
        String descripcion = req.getParameter("descripcion");

        PropiedadAdminService admin = admin();
        AlmacenImagenes almacen = servicio(AlmacenImagenes.class);
        String codigo = admin.obtener(id, Propiedad::getCodigo); // 404 si la propiedad no existe

        String url;
        try (InputStream datos = archivo.getInputStream()) {
            url = almacen.guardar(codigo, datos);
        }
        try {
            admin.agregarImagen(id, new ImagenRequest(url, descripcion));
        } catch (RuntimeException e) {
            // Si no se pudo registrar en la base, no queda un archivo huérfano
            almacen.eliminar(url);
            throw e;
        } finally {
            try {
                archivo.delete(); // temporales de Tomcat
            } catch (IOException e) {
                log.debug("No se pudo borrar el temporal de la subida", e);
            }
        }
        escribirJson(resp, HttpServletResponse.SC_CREATED, detalle(id));
    }

    // ---- Auxiliares ----

    private PropiedadAdminService admin() {
        return servicio(PropiedadAdminService.class);
    }

    private PropiedadDetalleDto detalle(long id) {
        return admin().obtener(id, PropiedadAdminMapper::detalle);
    }

    private static String ruta(HttpServletRequest req) {
        return req.getPathInfo() == null ? "" : req.getPathInfo();
    }

    private static boolean esColeccion(HttpServletRequest req) {
        String r = ruta(req);
        return r.isEmpty() || "/".equals(r);
    }

    /** Extrae el id de la propiedad si la ruta coincide con el patrón; si no, 404 o 405. */
    private static long idDe(Pattern patron, HttpServletRequest req) {
        Matcher m = patron.matcher(ruta(req));
        if (!m.matches()) {
            throw rutaInexistenteOMetodo(req);
        }
        return Long.parseLong(m.group(1));
    }

    /** La ruta existe pero con otro método → 405; si no existe con ningún método → 404. */
    private static ApiException rutaInexistenteOMetodo(HttpServletRequest req) {
        String r = ruta(req);
        boolean rutaConocida = r.isEmpty() || "/".equals(r) || PROPIEDAD.matcher(r).matches()
                || IMAGENES.matcher(r).matches() || IMAGEN.matcher(r).matches() || PRINCIPAL.matcher(r).matches();
        return rutaConocida ? new MetodoNoPermitidoException(req.getMethod())
                : new NoEncontradoException("El recurso solicitado no existe");
    }
}

package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.dto.request.CuentaRequest;
import py.sn.inmobiliaria.dto.request.UsuarioRequest;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.service.UsuarioService;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestión de usuarios del panel. Los permisos los exige AuthFilter (ReglasAcceso):
 * consultar requiere USUARIOS_VER (ADMIN y GERENTE); modificar, USUARIOS_GESTIONAR (solo ADMIN).
 *
 * <ul>
 *   <li>GET    /api/admin/usuarios                         listado</li>
 *   <li>GET    /api/admin/usuarios/{id}                    detalle</li>
 *   <li>POST   /api/admin/usuarios                         alta → 201 con la contraseña temporal (se muestra una vez)</li>
 *   <li>PUT    /api/admin/usuarios/{id}                    datos y rol</li>
 *   <li>PATCH  /api/admin/usuarios/{id}/estado             {"activo": true|false}</li>
 *   <li>POST   /api/admin/usuarios/{id}/contrasena-temporal nueva contraseña temporal</li>
 * </ul>
 */
@WebServlet(name = "UsuarioServlet", urlPatterns = {"/api/admin/usuarios", "/api/admin/usuarios/*"})
public class UsuarioServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^/([0-9]+)$");
    private static final Pattern ESTADO = Pattern.compile("^/([0-9]+)/estado$");
    private static final Pattern CONTRASENA = Pattern.compile("^/([0-9]+)/contrasena-temporal$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        if (ruta == null || ruta.equals("/")) {
            escribirJson(resp, HttpServletResponse.SC_OK, usuarios().listar());
            return;
        }
        escribirJson(resp, HttpServletResponse.SC_OK, usuarios().obtener(id(ID, ruta)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        if (ruta == null || ruta.equals("/")) {
            escribirJson(resp, HttpServletResponse.SC_CREATED, usuarios().crear(leerJson(req, UsuarioRequest.class)));
            return;
        }
        int id = id(CONTRASENA, ruta);
        escribirJson(resp, HttpServletResponse.SC_OK, usuarios().restablecerContrasena(id, PropiedadAdminServlet.actor(req).id()));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = id(ID, req.getPathInfo());
        escribirJson(resp, HttpServletResponse.SC_OK,
                usuarios().actualizar(id, leerJson(req, UsuarioRequest.class), PropiedadAdminServlet.actor(req).id()));
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = id(ESTADO, req.getPathInfo());
        CuentaRequest.Estado estado = leerJson(req, CuentaRequest.Estado.class);
        escribirJson(resp, HttpServletResponse.SC_OK,
                usuarios().cambiarEstado(id, estado.activo(), PropiedadAdminServlet.actor(req).id()));
    }

    private static int id(Pattern patron, String ruta) {
        Matcher m = patron.matcher(ruta == null ? "" : ruta);
        if (!m.matches()) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        return Integer.parseInt(m.group(1));
    }

    private UsuarioService usuarios() {
        return servicio(UsuarioService.class);
    }
}

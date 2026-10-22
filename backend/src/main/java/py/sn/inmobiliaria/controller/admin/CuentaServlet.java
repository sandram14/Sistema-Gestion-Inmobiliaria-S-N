package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.dto.request.CuentaRequest;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.UsuarioService;

import java.io.IOException;

/**
 * Cuenta propia (permiso CUENTA_PROPIA, que tienen todos los roles). Siempre actúa sobre el
 * usuario de la sesión: no recibe ids, así nadie puede modificar la cuenta de otro.
 *
 * <ul>
 *   <li>GET /api/admin/cuenta              mis datos</li>
 *   <li>PUT /api/admin/cuenta              nombre, apellido, cédula, teléfono</li>
 *   <li>PUT /api/admin/cuenta/email        nuevo email + contraseña actual</li>
 *   <li>PUT /api/admin/cuenta/contrasena   contraseña actual + nueva (quita la contraseña temporal)</li>
 * </ul>
 */
@WebServlet(name = "CuentaServlet", urlPatterns = {"/api/admin/cuenta", "/api/admin/cuenta/*"})
public class CuentaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getPathInfo() != null && !req.getPathInfo().equals("/")) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        escribirJson(resp, HttpServletResponse.SC_OK, servicio(UsuarioService.class).obtener(yo(req).id()));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UsuarioService usuarios = servicio(UsuarioService.class);
        int id = yo(req).id();
        String ruta = req.getPathInfo() == null ? "/" : req.getPathInfo();
        switch (ruta) {
            case "/" -> escribirJson(resp, HttpServletResponse.SC_OK,
                    usuarios.actualizarMisDatos(id, leerJson(req, CuentaRequest.Datos.class)));
            case "/email" -> escribirJson(resp, HttpServletResponse.SC_OK,
                    usuarios.cambiarMiEmail(id, leerJson(req, CuentaRequest.CambioEmail.class)));
            case "/contrasena" -> {
                usuarios.cambiarMiContrasena(id, leerJson(req, CuentaRequest.CambioContrasena.class));
                req.changeSessionId(); // nueva sesión tras cambiar la contraseña
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            default -> throw new NoEncontradoException("Recurso no encontrado");
        }
    }

    private static UsuarioSesion yo(HttpServletRequest req) {
        return PropiedadAdminServlet.actor(req);
    }
}

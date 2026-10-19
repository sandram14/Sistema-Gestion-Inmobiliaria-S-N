package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.dto.request.LoginRequest;
import py.sn.inmobiliaria.exception.NoAutenticadoException;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.AuthService;

import java.io.IOException;
import java.util.Map;

/**
 * Autenticación del panel administrativo (sesión HTTP con cookie HttpOnly):
 * <ul>
 *   <li>{@code POST /api/admin/auth/login}  → 200 {"usuario": {...}} y cookie de sesión</li>
 *   <li>{@code POST /api/admin/auth/logout} → 204 y cierra la sesión</li>
 *   <li>{@code GET  /api/admin/auth/me}     → 200 {"usuario": {...}} o 401 si no hay sesión</li>
 * </ul>
 */
@WebServlet(name = "AuthServlet", urlPatterns = "/api/admin/auth/*")
public class AuthServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (ruta(req)) {
            case "/login" -> login(req, resp);
            case "/logout" -> logout(req, resp);
            default -> throw new NoEncontradoException("El recurso solicitado no existe");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"/me".equals(ruta(req))) {
            throw new NoEncontradoException("El recurso solicitado no existe");
        }
        HttpSession sesion = req.getSession(false);
        Object usuario = sesion == null ? null : sesion.getAttribute(UsuarioSesion.ATRIBUTO);
        if (!(usuario instanceof UsuarioSesion u)) {
            throw new NoAutenticadoException("No hay una sesión activa");
        }
        escribirJson(resp, HttpServletResponse.SC_OK, Map.of("usuario", u));
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest datos = leerJson(req, LoginRequest.class);
        AuthService.Acceso acceso = servicio(AuthService.class).iniciarSesion(datos.email(), datos.password(), req.getRemoteAddr());
        UsuarioSesion usuario = acceso.usuario();

        // Sesión NUEVA (id nuevo) en cada login: si alguien conocía un id de sesión anterior,
        // deja de servir (protección contra "fijación de sesión").
        HttpSession anterior = req.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        HttpSession sesion = req.getSession(true);
        sesion.setAttribute(UsuarioSesion.ATRIBUTO, usuario);
        sesion.setAttribute(UsuarioSesion.ATRIBUTO_VERSION_CREDENCIAL, acceso.versionCredencial());

        escribirJson(resp, HttpServletResponse.SC_OK, Map.of("usuario", usuario));
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) {
        HttpSession sesion = req.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        escribirSinContenido(resp);
    }

    private static String ruta(HttpServletRequest req) {
        return req.getPathInfo() == null ? "" : req.getPathInfo();
    }
}

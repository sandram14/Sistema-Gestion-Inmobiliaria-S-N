package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.exception.NoEncontradoException;

/**
 * Atiende cualquier ruta {@code /api/*} que no tenga un Servlet propio y responde 404 en JSON.
 *
 * <p>Tomcat siempre elige el patrón más específico, así que {@code /api/health}
 * (y en adelante {@code /api/propiedades/*}, etc.) tienen prioridad sobre este.</p>
 */
@WebServlet(name = "ApiNoEncontradaServlet", urlPatterns = "/api/*")
public class ApiNoEncontradaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void despachar(HttpServletRequest req, HttpServletResponse resp) {
        throw new NoEncontradoException("El recurso solicitado no existe");
    }
}

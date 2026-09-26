package py.sn.inmobiliaria.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.response.HealthResponse;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * {@code GET /api/health}: confirma que el backend está desplegado y respondiendo.
 *
 * <p>En la Fase 4 se agregará el estado de la conexión con PostgreSQL.</p>
 */
@WebServlet(name = "HealthServlet", urlPatterns = "/api/health")
public class HealthServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private transient AppConfig config;

    @Override
    public void init() throws ServletException {
        config = (AppConfig) getServletContext().getAttribute(AppConfig.ATRIBUTO_CONTEXTO);
        if (config == null) {
            throw new ServletException("AppConfig no inicializada: revise AppContextListener");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HealthResponse respuesta = new HealthResponse(
                "ok",
                config.nombre(),
                config.version(),
                config.entorno(),
                System.getProperty("java.version"),
                OffsetDateTime.now());
        escribirJson(resp, HttpServletResponse.SC_OK, respuesta);
    }
}

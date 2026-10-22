package py.sn.inmobiliaria.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.dto.response.HealthResponse;
import py.sn.inmobiliaria.service.SaludService;

import java.io.IOException;

/**
 * {@code GET /api/health}: confirma que el backend está desplegado y si la base responde.
 *
 * <p>Responde 200 cuando todo funciona y 503 (Service Unavailable) cuando la base de
 * datos no responde o no está configurada, para que Docker u otras herramientas de
 * monitoreo lo detecten.</p>
 */
@WebServlet(name = "HealthServlet", urlPatterns = "/api/health")
public class HealthServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private transient SaludService saludService;

    @Override
    public void init() throws ServletException {
        saludService = (SaludService) getServletContext().getAttribute(SaludService.class.getName());
        if (saludService == null) {
            throw new ServletException("SaludService no inicializado: revise AppContextListener");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HealthResponse respuesta = saludService.verificar();
        int estadoHttp = "ok".equals(respuesta.estado())
                ? HttpServletResponse.SC_OK
                : HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        escribirJson(resp, estadoHttp, respuesta);
    }
}

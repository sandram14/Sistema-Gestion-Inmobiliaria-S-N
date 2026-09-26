package py.sn.inmobiliaria.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.dto.response.ErrorResponse;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.MetodoNoPermitidoException;
import py.sn.inmobiliaria.util.JsonUtil;

import java.io.IOException;

/**
 * Clase base de todos los Servlets de la API.
 *
 * <p>Centraliza tres cosas para que cada Servlet concreto solo se ocupe de su recurso:</p>
 * <ul>
 *   <li>escribir respuestas JSON;</li>
 *   <li>convertir cualquier error en el formato JSON único de la API (sin stack traces);</li>
 *   <li>responder 405 en JSON a los métodos HTTP que el Servlet no implementa
 *       (incluido PATCH, que la Servlet API 6.0 no despacha por sí sola).</li>
 * </ul>
 */
public abstract class ApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ApiServlet.class);

    private static final String TIPO_JSON = "application/json";

    @Override
    protected final void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            despachar(req, resp);
        } catch (ApiException e) {
            escribirError(resp, e.estadoHttp(), ErrorResponse.de(e.codigo(), e.getMessage(), e.campos()));
        } catch (Exception e) {
            // El detalle queda solo en el log del servidor; el cliente recibe un mensaje genérico.
            log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), e);
            escribirError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ErrorResponse.de("ERROR_INTERNO", "Ocurrió un error inesperado. Intente nuevamente."));
        }
    }

    /**
     * Envía la petición al método correspondiente (doGet, doPost, ..., doPatch).
     * Un Servlet puede sobrescribirlo para atender todos los métodos igual.
     */
    protected void despachar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    // ---- Métodos no implementados: 405 en JSON en lugar de la página HTML de Tomcat ----

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw new MetodoNoPermitidoException(req.getMethod());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw new MetodoNoPermitidoException(req.getMethod());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw new MetodoNoPermitidoException(req.getMethod());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw new MetodoNoPermitidoException(req.getMethod());
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        throw new MetodoNoPermitidoException(req.getMethod());
    }

    // ---- Utilidades de respuesta ----

    /** Escribe {@code cuerpo} como JSON con el código HTTP indicado. */
    protected void escribirJson(HttpServletResponse resp, int estadoHttp, Object cuerpo) throws IOException {
        resp.setStatus(estadoHttp);
        resp.setContentType(TIPO_JSON);
        resp.setCharacterEncoding("UTF-8");
        if (resp.getHeader("Cache-Control") == null) {
            // Por defecto las respuestas de la API no se guardan en caché
            resp.setHeader("Cache-Control", "no-store");
        }
        JsonUtil.mapper().writeValue(resp.getOutputStream(), cuerpo);
    }

    private void escribirError(HttpServletResponse resp, int estadoHttp, ErrorResponse error) throws IOException {
        if (resp.isCommitted()) {
            log.warn("No se pudo enviar el error {} porque la respuesta ya había comenzado", estadoHttp);
            return;
        }
        resp.reset(); // descarta cualquier salida parcial (reset también borra cabeceras)
        resp.setHeader("X-Content-Type-Options", "nosniff");
        escribirJson(resp, estadoHttp, error);
    }
}

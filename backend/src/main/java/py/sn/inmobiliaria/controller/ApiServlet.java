package py.sn.inmobiliaria.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.dto.response.ErrorResponse;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.MetodoNoPermitidoException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.util.JsonUtil;
import py.sn.inmobiliaria.util.RespuestaJson;

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

    // ---- Acceso a los services ----

    /**
     * Obtiene un service creado por AppContextListener. Si no existe (la base de datos no
     * está configurada), responde 503 en lugar de fallar con un error interno.
     */
    protected <T> T servicio(Class<T> tipo) {
        Object instancia = getServletContext().getAttribute(tipo.getName());
        if (instancia == null) {
            throw new ServicioNoDisponibleException(
                    "El servicio no está disponible: la base de datos no está configurada", null);
        }
        return tipo.cast(instancia);
    }

    // ---- Utilidades de respuesta ----

    /**
     * Permite que el navegador guarde la respuesta durante {@code segundos}.
     * Se usa para datos que casi no cambian (ciudades, polígonos de barrios).
     */
    protected void permitirCache(HttpServletResponse resp, int segundos) {
        resp.setHeader("Cache-Control", "public, max-age=" + segundos);
    }

    /** Escribe {@code cuerpo} como JSON con el código HTTP indicado. */
    protected void escribirJson(HttpServletResponse resp, int estadoHttp, Object cuerpo) throws IOException {
        RespuestaJson.escribir(resp, estadoHttp, cuerpo);
    }

    /** 204 No Content: la operación salió bien y no hay nada que devolver. */
    protected void escribirSinContenido(HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        resp.setHeader("Cache-Control", "no-store");
    }

    // ---- Lectura del cuerpo JSON ----

    /** Tamaño máximo aceptado para un cuerpo JSON (los formularios del panel ocupan pocos KB). */
    private static final int MAX_CUERPO = 64 * 1024;

    /**
     * Lee el cuerpo de la petición como JSON y lo convierte al DTO indicado.
     *
     * @throws ApiException 415 si no es JSON; 400 si está vacío, es demasiado grande,
     *                      está mal formado o trae campos desconocidos
     */
    protected <T> T leerJson(HttpServletRequest req, Class<T> tipo) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(TIPO_JSON)) {
            throw new ApiException(415, "TIPO_NO_SOPORTADO", "El cuerpo debe enviarse como application/json");
        }
        byte[] cuerpo = req.getInputStream().readNBytes(MAX_CUERPO + 1);
        if (cuerpo.length > MAX_CUERPO) {
            throw ValidacionException.deCampo("cuerpo", "El contenido es demasiado grande");
        }
        if (cuerpo.length == 0) {
            throw ValidacionException.deCampo("cuerpo", "No se recibieron datos");
        }
        try {
            T valor = JsonUtil.mapper().readValue(cuerpo, tipo);
            if (valor == null) {
                throw ValidacionException.deCampo("cuerpo", "No se recibieron datos");
            }
            return valor;
        } catch (UnrecognizedPropertyException e) {
            throw ValidacionException.deCampo(e.getPropertyName(), "Campo no permitido");
        } catch (JsonProcessingException e) {
            throw ValidacionException.deCampo("cuerpo", "El contenido no es un JSON válido o tiene tipos incorrectos");
        }
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

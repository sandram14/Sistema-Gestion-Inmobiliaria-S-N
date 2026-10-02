package py.sn.inmobiliaria.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Escritura de respuestas JSON, compartida por los Servlets y los filtros. */
public final class RespuestaJson {

    private RespuestaJson() {
    }

    public static void escribir(HttpServletResponse resp, int estadoHttp, Object cuerpo) throws IOException {
        resp.setStatus(estadoHttp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        if (resp.getHeader("Cache-Control") == null) {
            // Por defecto las respuestas de la API no se guardan en caché
            resp.setHeader("Cache-Control", "no-store");
        }
        JsonUtil.mapper().writeValue(resp.getOutputStream(), cuerpo);
    }
}

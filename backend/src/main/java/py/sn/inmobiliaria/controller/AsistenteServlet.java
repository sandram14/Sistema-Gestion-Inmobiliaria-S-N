package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.request.ConsultaAsistenteRequest;
import py.sn.inmobiliaria.dto.response.AsistenteResponse;
import py.sn.inmobiliaria.service.asistente.AsistenteService;

import java.io.IOException;
import java.util.UUID;

/**
 * {@code POST /api/asistente/consultas}: pregunta al asistente de IA (público, sin cuenta).
 *
 * <p>El visitante se identifica con una cookie anónima {@code sn_sesion} (un UUID aleatorio,
 * HttpOnly), distinta de la sesión del administrador. No se guarda la IP ni datos personales.
 * La cookie sirve para aplicar los límites de uso y agrupar las consultas en la tabla CONSULTA.</p>
 */
@WebServlet(name = "AsistenteServlet", urlPatterns = "/api/asistente/consultas")
public class AsistenteServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    public static final String COOKIE_SESION = "sn_sesion";
    private static final int TREINTA_DIAS = 30 * 24 * 60 * 60;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ConsultaAsistenteRequest datos = leerJson(req, ConsultaAsistenteRequest.class);
        UUID sesion = sesionDelVisitante(req, resp);
        AsistenteResponse respuesta = servicio(AsistenteService.class)
                .responder(sesion, datos.pregunta(), datos.codigoPropiedad());
        escribirJson(resp, HttpServletResponse.SC_OK, respuesta);
    }

    /** Reutiliza la cookie si tiene un UUID válido; si no, crea uno nuevo. */
    private UUID sesionDelVisitante(HttpServletRequest req, HttpServletResponse resp) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_SESION.equals(c.getName())) {
                    try {
                        return UUID.fromString(c.getValue());
                    } catch (IllegalArgumentException e) {
                        break; // valor manipulado: se reemplaza
                    }
                }
            }
        }
        UUID nueva = UUID.randomUUID();
        Cookie cookie = new Cookie(COOKIE_SESION, nueva.toString());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(TREINTA_DIAS);
        cookie.setAttribute("SameSite", "Lax");
        Object config = getServletContext().getAttribute(AppConfig.ATRIBUTO_CONTEXTO);
        if (config instanceof AppConfig c && c.esProduccion()) {
            cookie.setSecure(true);
        }
        resp.addCookie(cookie);
        return nueva;
    }
}

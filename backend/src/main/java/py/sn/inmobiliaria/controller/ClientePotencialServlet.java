package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.dto.ClienteDtos;
import py.sn.inmobiliaria.exception.DemasiadasConsultasException;

import py.sn.inmobiliaria.service.clientes.ClientePotencialService;
import py.sn.inmobiliaria.service.clientes.LimitadorEnvios;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

/**
 * POST /api/clientes-potenciales (público): el visitante deja sus datos para ser contactado.
 * Exige la aceptación del tratamiento. Límite: 3 envíos por hora por visitante y 100 por día en total.
 * La IP solo se usa en memoria para el límite; no se guarda. Contrato: docs/integracion-clientes-potenciales.md
 */
@WebServlet(name = "ClientePotencialServlet", urlPatterns = "/api/clientes-potenciales")
public class ClientePotencialServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final long HORA = 3_600_000L;
    private final transient LimitadorEnvios porVisitante = new LimitadorEnvios(3, HORA, Clock.systemUTC());
    private final transient LimitadorEnvios global = new LimitadorEnvios(100, 24 * HORA, Clock.systemUTC());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UUID sesion = sesion(req);
        String clave = (sesion == null ? "-" : sesion.toString()) + "|" + req.getRemoteAddr();
        if (!porVisitante.permitir(clave) || !global.permitir("*")) {
            throw new DemasiadasConsultasException("Recibimos varios envíos seguidos. Intente nuevamente más tarde o escríbanos por WhatsApp.");
        }
        servicio(ClientePotencialService.class).registrarDesdeChat(leerJson(req, ClienteDtos.Solicitud.class), sesion);
        escribirJson(resp, HttpServletResponse.SC_CREATED, Map.of("registrado", true,
                "mensaje", "Gracias. Un asesor se pondrá en contacto con usted."));
    }

    private static UUID sesion(HttpServletRequest req) {
        if (req.getCookies() == null) {
            return null;
        }
        for (Cookie c : req.getCookies()) {
            if (AsistenteServlet.COOKIE_SESION.equals(c.getName())) {
                try {
                    return UUID.fromString(c.getValue());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
}

package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.service.sitio.ContenidoSitioService;

import java.io.IOException;

/**
 * GET /api/sitio (público): contenido editable del sitio (inicio, servicios, nosotros, contacto),
 * contactoProvisional y los códigos de las ofertas destacadas que siguen disponibles, en orden.
 * Contrato: docs/integracion-gestion-sitio.md
 */
@WebServlet(name = "SitioServlet", urlPatterns = "/api/sitio")
public class SitioServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "public, max-age=60"); // un cambio guardado se ve en, como máximo, un minuto
        escribirJson(resp, HttpServletResponse.SC_OK, servicio(ContenidoSitioService.class).publico());
    }
}

package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.service.consultas.ConsultaAdminService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consultas del chat IA en el panel. Permisos (ReglasAcceso): historial, detalle y revisión con
 * CONSULTAS_VER (el vendedor ve solo las suyas y las generales); análisis con CONSULTAS_VER_TODAS.
 *
 * <ul>
 *   <li>GET   /api/admin/consultas?texto=&desde=AAAA-MM-DD&hasta=AAAA-MM-DD&estado=&resultado=&pagina=  (20 por página)</li>
 *   <li>GET   /api/admin/consultas/{id}</li>
 *   <li>PATCH /api/admin/consultas/{id}/revision   {"estado": "REVISADA" | "PENDIENTE"}</li>
 *   <li>GET   /api/admin/consultas/analisis?desde=&hasta=</li>
 * </ul>
 * Las fechas se interpretan en la hora de Paraguay; "hasta" incluye ese día completo.
 */
@WebServlet(name = "ConsultaAdminServlet", urlPatterns = {"/api/admin/consultas", "/api/admin/consultas/*"})
public class ConsultaAdminServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final ZoneId PARAGUAY = ZoneId.of("America/Asuncion");
    private static final Pattern ID = Pattern.compile("^/([0-9]+)$");
    private static final Pattern REVISION = Pattern.compile("^/([0-9]+)/revision$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo() == null ? "/" : req.getPathInfo();
        OffsetDateTime desde = fecha(req, "desde", false);
        OffsetDateTime hasta = fecha(req, "hasta", true);
        if ("/".equals(ruta)) {
            escribirJson(resp, HttpServletResponse.SC_OK, consultas().historial(req.getParameter("texto"), desde, hasta,
                    req.getParameter("estado"), req.getParameter("resultado"), pagina(req), PropiedadAdminServlet.actor(req)));
        } else if ("/analisis".equals(ruta)) {
            escribirJson(resp, HttpServletResponse.SC_OK, consultas().analisis(desde, hasta));
        } else {
            escribirJson(resp, HttpServletResponse.SC_OK, consultas().detalle(id(ID, ruta), PropiedadAdminServlet.actor(req)));
        }
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = id(REVISION, req.getPathInfo());
        @SuppressWarnings("unchecked")
        Map<String, Object> cuerpo = leerJson(req, Map.class);
        Object estado = cuerpo.get("estado");
        escribirJson(resp, HttpServletResponse.SC_OK,
                consultas().revisar(id, estado == null ? null : estado.toString(), PropiedadAdminServlet.actor(req)));
    }

    /** "AAAA-MM-DD" en hora de Paraguay. "hasta" se convierte en el inicio del día siguiente (exclusivo). */
    private static OffsetDateTime fecha(HttpServletRequest req, String nombre, boolean finDelDia) {
        String valor = req.getParameter(nombre);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(valor.strip());
            return (finDelDia ? d.plusDays(1) : d).atStartOfDay(PARAGUAY).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            throw ValidacionException.deCampo(nombre, "Use el formato AAAA-MM-DD");
        }
    }

    private static int pagina(HttpServletRequest req) {
        String v = req.getParameter("pagina");
        try {
            return v == null || v.isBlank() ? 1 : Integer.parseInt(v.strip());
        } catch (NumberFormatException e) {
            throw ValidacionException.deCampo("pagina", "Debe ser un número");
        }
    }

    private static long id(Pattern patron, String ruta) {
        Matcher m = patron.matcher(ruta == null ? "" : ruta);
        if (!m.matches()) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        return Long.parseLong(m.group(1));
    }

    private ConsultaAdminService consultas() {
        return servicio(ConsultaAdminService.class);
    }
}

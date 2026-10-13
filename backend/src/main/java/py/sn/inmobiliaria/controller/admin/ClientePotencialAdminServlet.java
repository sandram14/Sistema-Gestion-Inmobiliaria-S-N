package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.dto.ClienteDtos;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.service.clientes.ClientePotencialService;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clientes potenciales en el panel. Permisos (ReglasAcceso): CLIENTES_VER para consultar, registrar a mano y
 * cambiar el estado (el vendedor, solo los asignados a él); CLIENTES_ASIGNAR para asignar responsable.
 * <ul>
 *   <li>GET   /api/admin/clientes-potenciales?texto=&estado=&origen=&pagina=</li>
 *   <li>GET   /api/admin/clientes-potenciales/{id}</li>
 *   <li>POST  /api/admin/clientes-potenciales            alta manual (origen WHATSAPP, LLAMADA u OTRO)</li>
 *   <li>PATCH /api/admin/clientes-potenciales/{id}/estado       {"estado": "PENDIENTE" | "CONTACTADO" | "DESCARTADO"}</li>
 *   <li>PATCH /api/admin/clientes-potenciales/{id}/responsable  {"idResponsable": 5 | null}</li>
 * </ul>
 */
@WebServlet(name = "ClientePotencialAdminServlet", urlPatterns = {"/api/admin/clientes-potenciales", "/api/admin/clientes-potenciales/*"})
public class ClientePotencialAdminServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern ID = Pattern.compile("^/([0-9]+)$");
    private static final Pattern ACCION = Pattern.compile("^/([0-9]+)/(estado|responsable)$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        if (ruta == null || ruta.equals("/")) {
            int pagina;
            try {
                pagina = req.getParameter("pagina") == null ? 1 : Integer.parseInt(req.getParameter("pagina"));
            } catch (NumberFormatException e) {
                throw ValidacionException.deCampo("pagina", "Debe ser un número");
            }
            escribirJson(resp, HttpServletResponse.SC_OK, clientes().listar(req.getParameter("texto"), req.getParameter("estado"),
                    req.getParameter("origen"), pagina, PropiedadAdminServlet.actor(req)));
            return;
        }
        Matcher m = ID.matcher(ruta);
        if (!m.matches()) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        escribirJson(resp, HttpServletResponse.SC_OK, clientes().obtener(Long.parseLong(m.group(1)), PropiedadAdminServlet.actor(req)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getPathInfo() != null && !req.getPathInfo().equals("/")) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        escribirJson(resp, HttpServletResponse.SC_CREATED,
                clientes().registrarManual(leerJson(req, ClienteDtos.Manual.class), PropiedadAdminServlet.actor(req)));
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Matcher m = ACCION.matcher(req.getPathInfo() == null ? "" : req.getPathInfo());
        if (!m.matches()) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
        long id = Long.parseLong(m.group(1));
        @SuppressWarnings("unchecked")
        Map<String, Object> cuerpo = leerJson(req, Map.class);
        var actor = PropiedadAdminServlet.actor(req);
        if ("estado".equals(m.group(2))) {
            Object e = cuerpo.get("estado");
            escribirJson(resp, HttpServletResponse.SC_OK, clientes().cambiarEstado(id, e == null ? null : e.toString(), actor));
        } else {
            Object r = cuerpo.get("idResponsable");
            Integer idResp = r == null ? null : (r instanceof Number n ? n.intValue() : null);
            if (r != null && idResp == null) {
                throw ValidacionException.deCampo("idResponsable", "Debe ser un número o null");
            }
            escribirJson(resp, HttpServletResponse.SC_OK, clientes().asignar(id, idResp, actor));
        }
    }

    private ClientePotencialService clientes() {
        return servicio(ClientePotencialService.class);
    }
}

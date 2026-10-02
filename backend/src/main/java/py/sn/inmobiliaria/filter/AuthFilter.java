package py.sn.inmobiliaria.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.response.ErrorResponse;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.util.RespuestaJson;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Protege todo {@code /api/admin/*}. Es el "guardia de la puerta" del panel: revisa cada
 * petición antes de que llegue a cualquier Servlet administrativo.
 *
 * <ol>
 *   <li><b>Protección CSRF</b> (que otro sitio web use la sesión del administrador sin que
 *       lo sepa): en POST/PUT/PATCH/DELETE exige un Content-Type que un formulario HTML de
 *       otro sitio no puede enviar sin permiso (application/json o multipart/form-data) y, si
 *       el navegador envía la cabecera Origin, que coincida con este mismo sitio. A esto se
 *       suma la cookie SameSite=Lax (META-INF/context.xml).</li>
 *   <li><b>Autenticación</b>: salvo login y logout, exige una sesión con un administrador.</li>
 * </ol>
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/api/admin/*")
public class AuthFilter implements Filter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of("/api/admin/auth/login", "/api/admin/auth/logout");
    private static final Set<String> METODOS_QUE_MODIFICAN = Set.of("POST", "PUT", "PATCH", "DELETE");

    private Set<String> origenesPermitidos = Set.of();

    @Override
    public void init(FilterConfig filterConfig) {
        Object config = filterConfig.getServletContext().getAttribute(AppConfig.ATRIBUTO_CONTEXTO);
        if (config instanceof AppConfig c) {
            origenesPermitidos = c.origenesPermitidos();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String ruta = req.getRequestURI().substring(req.getContextPath().length());
        String metodo = req.getMethod().toUpperCase(Locale.ROOT);

        if (METODOS_QUE_MODIFICAN.contains(metodo)) {
            if (!origenPermitido(req)) {
                rechazar(resp, 403, "ACCESO_DENEGADO", "Origen de la petición no permitido");
                return;
            }
            boolean logoutSinCuerpo = ruta.equals("/api/admin/auth/logout") && req.getContentLengthLong() <= 0;
            if (!logoutSinCuerpo && !contentTypeSeguro(req.getContentType())) {
                rechazar(resp, 415, "TIPO_NO_SOPORTADO", "El cuerpo debe enviarse como application/json");
                return;
            }
        }

        if (!RUTAS_PUBLICAS.contains(ruta)) {
            HttpSession sesion = req.getSession(false);
            Object usuario = sesion == null ? null : sesion.getAttribute(UsuarioSesion.ATRIBUTO);
            if (!(usuario instanceof UsuarioSesion u)) {
                rechazar(resp, 401, "NO_AUTENTICADO", "Inicie sesión para continuar");
                return;
            }
            if (!u.esAdmin()) {
                rechazar(resp, 403, "ACCESO_DENEGADO", "No tiene permisos para esta operación");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Sin cabecera Origin (herramientas como curl) se permite: la cookie SameSite ya impide
     * que otro sitio la envíe. Con Origin, su host:puerto debe ser el de esta misma
     * aplicación (cabecera Host) o uno de APP_ORIGENES_PERMITIDOS.
     */
    private boolean origenPermitido(HttpServletRequest req) {
        String origen = req.getHeader("Origin");
        if (origen == null || origen.isBlank()) {
            return true;
        }
        if (origenesPermitidos.contains(origen.trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        try {
            String autoridad = URI.create(origen.trim()).getRawAuthority();
            String host = req.getHeader("Host");
            return autoridad != null && host != null && autoridad.equalsIgnoreCase(host.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean contentTypeSeguro(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        return ct.startsWith("application/json") || ct.startsWith("multipart/form-data");
    }

    private static void rechazar(HttpServletResponse resp, int estado, String codigo, String mensaje) throws IOException {
        RespuestaJson.escribir(resp, estado, ErrorResponse.de(codigo, mensaje));
    }
}

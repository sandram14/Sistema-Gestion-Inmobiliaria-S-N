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
import py.sn.inmobiliaria.seguridad.Permiso;
import py.sn.inmobiliaria.seguridad.ReglasAcceso;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.AuthService;
import py.sn.inmobiliaria.util.RespuestaJson;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Protege todo {@code /api/admin/*}. Es el "guardia de la puerta" del panel: revisa cada
 * petición antes de que llegue a cualquier Servlet administrativo.
 *
 * <ol>
 *   <li><b>Protección CSRF</b> (que otro sitio web use la sesión del administrador sin que
 *       lo sepa): en POST/PUT/PATCH/DELETE, si el navegador envía la cabecera Origin, debe
 *       coincidir con este mismo sitio; y si la petición trae cuerpo, su Content-Type debe ser
 *       uno que un formulario HTML de otro sitio no puede enviar sin permiso
 *       (application/json o multipart/form-data). Las peticiones sin cuerpo (por ejemplo,
 *       DELETE o PATCH .../principal) no pueden transportar un formulario. A esto se suma la
 *       cookie SameSite=Lax (META-INF/context.xml).</li>
 *   <li><b>Autenticación</b>: salvo login y logout, exige una sesión, y relee el usuario de la
 *       base en cada petición (una cuenta desactivada o con otro rol pierde el acceso enseguida).</li>
 *   <li><b>Autorización</b>: cada ruta exige un permiso según {@link ReglasAcceso}; el rol del
 *       usuario debe tenerlo ({@link Permiso}). Una ruta sin regla queda bloqueada (403).</li>
 * </ol>
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = "/api/admin/*")
public class AuthFilter implements Filter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of("/api/admin/auth/login", "/api/admin/auth/logout");
    private static final Set<String> METODOS_QUE_MODIFICAN = Set.of("POST", "PUT", "PATCH", "DELETE");

    private Set<String> origenesPermitidos = Set.of();
    private jakarta.servlet.ServletContext contexto;

    @Override
    public void init(FilterConfig filterConfig) {
        contexto = filterConfig.getServletContext();
        Object config = contexto.getAttribute(AppConfig.ATRIBUTO_CONTEXTO);
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
            if (tieneCuerpo(req) && !contentTypeSeguro(req.getContentType())) {
                rechazar(resp, 415, "TIPO_NO_SOPORTADO", "El cuerpo debe enviarse como application/json");
                return;
            }
        }

        if (!RUTAS_PUBLICAS.contains(ruta)) {
            HttpSession sesion = req.getSession(false);
            Object guardado = sesion == null ? null : sesion.getAttribute(UsuarioSesion.ATRIBUTO);
            if (!(guardado instanceof UsuarioSesion enSesion)) {
                rechazar(resp, 401, "NO_AUTENTICADO", "Inicie sesión para continuar");
                return;
            }

            // 1. Se relee el usuario de la base: una cuenta desactivada o con otro rol
            //    pierde el acceso en la siguiente petición, sin esperar a que venza la sesión.
            Object servicio = contexto.getAttribute(AuthService.class.getName());
            if (!(servicio instanceof AuthService auth)) {
                rechazar(resp, 503, "SERVICIO_NO_DISPONIBLE", "La base de datos no está disponible");
                return;
            }
            Optional<UsuarioSesion> actual;
            try {
                actual = auth.recargar(enSesion.id());
            } catch (RuntimeException e) {
                rechazar(resp, 503, "SERVICIO_NO_DISPONIBLE", "La base de datos no está disponible");
                return;
            }
            if (actual.isEmpty()) {
                sesion.invalidate();
                rechazar(resp, 401, "NO_AUTENTICADO", "Su cuenta no está activa. Inicie sesión nuevamente");
                return;
            }
            UsuarioSesion u = actual.get();
            if (!u.equals(enSesion)) {
                sesion.setAttribute(UsuarioSesion.ATRIBUTO, u);
            }

            // 2. Permiso que exige la ruta. Sin regla declarada, la ruta queda bloqueada.
            Optional<Permiso> exigido = ReglasAcceso.permisoPara(metodo, ruta);
            if (exigido.isEmpty() || !u.tiene(exigido.get())) {
                rechazar(resp, 403, "ACCESO_DENEGADO", "No tiene permisos para esta operación");
                return;
            }
            req.setAttribute(UsuarioSesion.ATRIBUTO_PETICION, u);
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

    private static boolean tieneCuerpo(HttpServletRequest req) {
        return req.getContentLengthLong() > 0 || req.getHeader("Transfer-Encoding") != null;
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

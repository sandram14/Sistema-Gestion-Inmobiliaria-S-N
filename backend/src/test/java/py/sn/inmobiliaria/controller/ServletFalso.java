package py.sn.inmobiliaria.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import py.sn.inmobiliaria.util.JsonUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Ejecuta Servlets y filtros en memoria, sin Tomcat: arma peticiones HTTP falsas (con
 * cabeceras, cuerpo y sesión), captura la respuesta y permite inspeccionar el JSON.
 * Usa "proxies dinámicos" de Java en lugar de una librería de mocks.
 */
public final class ServletFalso {

    private ServletFalso() {
    }

    // =====================================================================
    // Respuesta capturada
    // =====================================================================

    public static final class Respuesta {
        public int estado = 200;
        public final Map<String, String> cabeceras = new HashMap<>();
        public final ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        public final java.util.List<jakarta.servlet.http.Cookie> cookies = new java.util.ArrayList<>();

        public JsonNode json() throws Exception {
            return JsonUtil.mapper().readTree(cuerpo.toString(StandardCharsets.UTF_8));
        }

        public String cabecera(String nombre) {
            return cabeceras.get(nombre.toLowerCase(Locale.ROOT));
        }
    }

    // =====================================================================
    // "Navegador": conserva la sesión entre peticiones, como lo haría la cookie
    // =====================================================================

    public static final class Navegador {
        Sesion sesion;

        public Sesion sesion() {
            return sesion;
        }

        /** Simula una sesión previa sin login (por ejemplo, un id de sesión "fijado" por un atacante). */
        public Sesion crearSesionAnonima() {
            sesion = new Sesion();
            return sesion;
        }
    }

    public static final class Sesion {
        public final String id = UUID.randomUUID().toString();
        public final Map<String, Object> atributos = new HashMap<>();
        boolean invalidada;
    }

    // =====================================================================
    // Petición
    // =====================================================================

    public static final class Peticion {
        String metodo = "GET";
        String servletPath;
        String pathInfo;
        final Map<String, String> parametros = new HashMap<>();
        final Map<String, String> cabeceras = new HashMap<>();
        byte[] cuerpo;
        String ip = "127.0.0.1";
        Navegador navegador = new Navegador();
        final Map<String, byte[]> partes = new HashMap<>();
        final java.util.List<jakarta.servlet.http.Cookie> cookies = new java.util.ArrayList<>();

        public static Peticion de(String metodo, String servletPath, String pathInfo) {
            Peticion p = new Peticion();
            p.metodo = metodo;
            p.servletPath = servletPath;
            p.pathInfo = pathInfo;
            return p;
        }

        public Peticion parametros(Map<String, String> valores) {
            parametros.putAll(valores);
            return this;
        }

        public Peticion cabecera(String nombre, String valor) {
            cabeceras.put(nombre.toLowerCase(Locale.ROOT), valor);
            return this;
        }

        /** Cuerpo JSON con su Content-Type. */
        public Peticion json(String json) {
            return cuerpo(json, "application/json");
        }

        public Peticion cuerpo(String contenido, String contentType) {
            cuerpo = contenido.getBytes(StandardCharsets.UTF_8);
            if (contentType != null) {
                cabecera("Content-Type", contentType);
            }
            return this;
        }

        public Peticion cookie(String nombre, String valor) {
            cookies.add(new jakarta.servlet.http.Cookie(nombre, valor));
            return this;
        }

        /** Agrega un archivo a una petición multipart/form-data. */
        public Peticion parte(String nombre, byte[] contenido) {
            partes.put(nombre, contenido);
            cabeceras.putIfAbsent("content-type", "multipart/form-data; boundary=prueba");
            return this;
        }

        public Peticion navegador(Navegador n) {
            navegador = n;
            return this;
        }

        public Peticion ip(String valor) {
            ip = valor;
            return this;
        }
    }

    // =====================================================================
    // Inicialización
    // =====================================================================

    private static ServletContext contexto(Map<String, Object> atributos) {
        return (ServletContext) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (p, m, args) -> switch (m.getName()) {
                    case "getAttribute" -> atributos.get((String) args[0]);
                    default -> null;
                });
    }

    private static Map<String, Object> atributos(Map<Class<?>, Object> objetos) {
        Map<String, Object> atributos = new HashMap<>();
        objetos.forEach((tipo, instancia) -> atributos.put(
                tipo == py.sn.inmobiliaria.config.AppConfig.class ? py.sn.inmobiliaria.config.AppConfig.ATRIBUTO_CONTEXTO
                        : tipo.getName(), instancia));
        return atributos;
    }

    /** Inicializa el Servlet con un ServletContext que contiene los objetos indicados. */
    public static <S extends HttpServlet> S iniciar(S servlet, Map<Class<?>, Object> servicios) throws Exception {
        ServletContext ctx = contexto(atributos(servicios));
        servlet.init((ServletConfig) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{ServletConfig.class}, (p, m, args) -> switch (m.getName()) {
                    case "getServletContext" -> ctx;
                    case "getServletName" -> servlet.getClass().getSimpleName();
                    case "getInitParameterNames" -> Collections.emptyEnumeration();
                    default -> null;
                }));
        return servlet;
    }

    public static <F extends Filter> F iniciarFiltro(F filtro, Map<Class<?>, Object> objetos) throws Exception {
        ServletContext ctx = contexto(atributos(objetos));
        filtro.init((FilterConfig) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{FilterConfig.class}, (p, m, args) -> switch (m.getName()) {
                    case "getServletContext" -> ctx;
                    case "getInitParameterNames" -> Collections.emptyEnumeration();
                    default -> null;
                }));
        return filtro;
    }

    // =====================================================================
    // Ejecución
    // =====================================================================

    public static Respuesta ejecutar(HttpServlet servlet, Peticion pet) throws Exception {
        Respuesta r = new Respuesta();
        servlet.service(peticion(pet), respuesta(r));
        return r;
    }

    /** Resultado de pasar por un filtro: la respuesta y si dejó continuar la petición. */
    public record ResultadoFiltro(Respuesta respuesta, boolean continuo) {
    }

    public static ResultadoFiltro filtrar(Filter filtro, Peticion pet) throws Exception {
        Respuesta r = new Respuesta();
        boolean[] continuo = {false};
        FilterChain cadena = (req, resp) -> continuo[0] = true;
        filtro.doFilter(peticion(pet), respuesta(r), cadena);
        return new ResultadoFiltro(r, continuo[0]);
    }

    // Atajos usados por las pruebas de la API pública
    public static Respuesta ejecutar(HttpServlet servlet, String metodo, String servletPath, String pathInfo,
                                     Map<String, String> parametros) throws Exception {
        return ejecutar(servlet, Peticion.de(metodo, servletPath, pathInfo).parametros(parametros));
    }

    public static Respuesta get(HttpServlet servlet, String servletPath, String pathInfo, Map<String, String> parametros)
            throws Exception {
        return ejecutar(servlet, "GET", servletPath, pathInfo, parametros);
    }

    // =====================================================================
    // Proxies
    // =====================================================================

    private static HttpSession sesion(Navegador nav, Sesion s) {
        return (HttpSession) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, (p, m, args) -> switch (m.getName()) {
                    case "getId" -> s.id;
                    case "getAttribute" -> s.atributos.get((String) args[0]);
                    case "setAttribute" -> s.atributos.put((String) args[0], args[1]);
                    case "removeAttribute" -> s.atributos.remove((String) args[0]);
                    case "invalidate" -> {
                        s.invalidada = true;
                        s.atributos.clear();
                        if (nav.sesion == s) {
                            nav.sesion = null;
                        }
                        yield null;
                    }
                    case "isNew" -> false;
                    case "hashCode" -> System.identityHashCode(s);
                    case "equals" -> p == args[0];
                    default -> null;
                });
    }

    private static jakarta.servlet.http.Part parte(Map<String, byte[]> partes, String nombre) {
        byte[] contenido = partes.get(nombre);
        if (contenido == null) {
            return null;
        }
        return (jakarta.servlet.http.Part) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{jakarta.servlet.http.Part.class}, (p, m, args) -> switch (m.getName()) {
                    case "getInputStream" -> new ByteArrayInputStream(contenido);
                    case "getSize" -> (long) contenido.length;
                    case "getName" -> nombre;
                    case "getSubmittedFileName" -> "foto.jpg";
                    default -> null;
                });
    }

    private static HttpServletRequest peticion(Peticion pet) {
        String uri = pet.servletPath + (pet.pathInfo == null ? "" : pet.pathInfo);
        ServletInputStream entrada = new ServletInputStream() {
            private final ByteArrayInputStream datos = new ByteArrayInputStream(pet.cuerpo == null ? new byte[0] : pet.cuerpo);

            @Override
            public boolean isFinished() {
                return datos.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
            }

            @Override
            public int read() {
                return datos.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return datos.read(b, off, len);
            }
        };
        return (HttpServletRequest) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, args) -> switch (m.getName()) {
                    case "getMethod" -> pet.metodo;
                    case "getServletPath" -> pet.servletPath;
                    case "getPathInfo" -> pet.pathInfo;
                    case "getRequestURI" -> uri;
                    case "getContextPath" -> "";
                    case "getParameter" -> pet.parametros.get((String) args[0]);
                    case "getHeader" -> pet.cabeceras.get(((String) args[0]).toLowerCase(Locale.ROOT));
                    case "getContentType" -> pet.cabeceras.get("content-type");
                    case "getContentLengthLong" -> pet.cuerpo == null ? -1L : (long) pet.cuerpo.length;
                    case "getContentLength" -> pet.cuerpo == null ? -1 : pet.cuerpo.length;
                    case "getInputStream" -> entrada;
                    case "getRemoteAddr" -> pet.ip;
                    case "getPart" -> parte(pet.partes, (String) args[0]);
                    case "getCookies" -> pet.cookies.isEmpty() ? null
                            : pet.cookies.toArray(new jakarta.servlet.http.Cookie[0]);
                    case "getProtocol" -> "HTTP/1.1";
                    case "getDateHeader" -> -1L;
                    case "getSession" -> {
                        boolean crear = args == null || args.length == 0 || (Boolean) args[0];
                        Navegador nav = pet.navegador;
                        if (nav.sesion != null && !nav.sesion.invalidada) {
                            yield sesion(nav, nav.sesion);
                        }
                        if (!crear) {
                            yield null;
                        }
                        nav.sesion = new Sesion();
                        yield sesion(nav, nav.sesion);
                    }
                    default -> null;
                });
    }

    private static HttpServletResponse respuesta(Respuesta r) {
        ServletOutputStream salida = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
            }

            @Override
            public void write(int b) {
                r.cuerpo.write(b);
            }
        };
        return (HttpServletResponse) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, args) -> {
                    switch (m.getName()) {
                        case "setStatus" -> r.estado = (Integer) args[0];
                        case "getStatus" -> {
                            return r.estado;
                        }
                        case "setHeader", "addHeader" ->
                                r.cabeceras.put(((String) args[0]).toLowerCase(Locale.ROOT), (String) args[1]);
                        case "getHeader" -> {
                            return r.cabeceras.get(((String) args[0]).toLowerCase(Locale.ROOT));
                        }
                        case "setContentType" -> r.cabeceras.put("content-type", (String) args[0]);
                        case "addCookie" -> r.cookies.add((jakarta.servlet.http.Cookie) args[0]);
                        case "getOutputStream" -> {
                            return salida;
                        }
                        case "isCommitted" -> {
                            return false;
                        }
                        case "reset" -> {
                            r.cabeceras.clear();
                            r.cuerpo.reset();
                        }
                        default -> {
                            return null;
                        }
                    }
                    return null;
                });
    }
}

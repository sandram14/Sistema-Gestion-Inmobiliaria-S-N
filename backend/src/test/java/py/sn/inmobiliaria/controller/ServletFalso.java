package py.sn.inmobiliaria.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.util.JsonUtil;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Ejecuta un Servlet en memoria, sin Tomcat: arma una petición HTTP falsa, captura la
 * respuesta (código, cabeceras y cuerpo) y permite inspeccionar el JSON devuelto.
 * Usa "proxies dinámicos" de Java en lugar de una librería de mocks.
 */
final class ServletFalso {

    private ServletFalso() {
    }

    /** Respuesta capturada. */
    static final class Respuesta {
        int estado = 200;
        final Map<String, String> cabeceras = new HashMap<>();
        final ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();

        JsonNode json() throws Exception {
            return JsonUtil.mapper().readTree(cuerpo.toString(StandardCharsets.UTF_8));
        }

        String cabecera(String nombre) {
            return cabeceras.get(nombre.toLowerCase());
        }
    }

    /** Inicializa el Servlet con un ServletContext que contiene los services indicados. */
    static <S extends HttpServlet> S iniciar(S servlet, Map<Class<?>, Object> servicios) throws Exception {
        Map<String, Object> atributos = new HashMap<>();
        servicios.forEach((tipo, instancia) -> atributos.put(tipo.getName(), instancia));
        ServletContext contexto = (ServletContext) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (p, m, args) -> switch (m.getName()) {
                    case "getAttribute" -> atributos.get((String) args[0]);
                    case "log" -> null;
                    default -> null;
                });
        ServletConfig config = (ServletConfig) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{ServletConfig.class}, (p, m, args) -> switch (m.getName()) {
                    case "getServletContext" -> contexto;
                    case "getServletName" -> servlet.getClass().getSimpleName();
                    case "getInitParameterNames" -> Collections.emptyEnumeration();
                    default -> null;
                });
        servlet.init(config);
        return servlet;
    }

    /**
     * @param servletPath parte de la URL que corresponde al Servlet (/api/propiedades)
     * @param pathInfo    resto de la ruta (/prop-001) o null
     */
    static Respuesta ejecutar(HttpServlet servlet, String metodo, String servletPath, String pathInfo,
                              Map<String, String> parametros) throws Exception {
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (p, m, args) -> switch (m.getName()) {
                    case "getMethod" -> metodo;
                    case "getServletPath" -> servletPath;
                    case "getPathInfo" -> pathInfo;
                    case "getRequestURI" -> servletPath + (pathInfo == null ? "" : pathInfo);
                    case "getParameter" -> parametros.get((String) args[0]);
                    case "getProtocol" -> "HTTP/1.1";
                    case "getDateHeader" -> -1L;
                    default -> null;
                });

        Respuesta r = new Respuesta();
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
        HttpServletResponse resp = (HttpServletResponse) Proxy.newProxyInstance(ServletFalso.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class}, (p, m, args) -> {
                    switch (m.getName()) {
                        case "setStatus" -> r.estado = (Integer) args[0];
                        case "getStatus" -> {
                            return r.estado;
                        }
                        case "setHeader", "addHeader" -> r.cabeceras.put(((String) args[0]).toLowerCase(), (String) args[1]);
                        case "getHeader" -> {
                            return r.cabeceras.get(((String) args[0]).toLowerCase());
                        }
                        case "setContentType" -> r.cabeceras.put("content-type", (String) args[0]);
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

        servlet.service(req, resp);
        return r;
    }

    static Respuesta get(HttpServlet servlet, String servletPath, String pathInfo, Map<String, String> parametros)
            throws Exception {
        return ejecutar(servlet, "GET", servletPath, pathInfo, parametros);
    }
}

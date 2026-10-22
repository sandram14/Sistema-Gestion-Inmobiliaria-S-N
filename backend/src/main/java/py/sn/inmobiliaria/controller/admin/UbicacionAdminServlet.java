package py.sn.inmobiliaria.controller.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.controller.ApiServlet;
import py.sn.inmobiliaria.mapper.CatalogoMapper;
import py.sn.inmobiliaria.service.UbicacionService;
import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.util.Parametros;

import java.io.IOException;

/**
 * {@code GET /api/admin/ubicacion?latitud=&longitud=}: el administrador hace clic en el mapa
 * y el sistema detecta con PostGIS el barrio, la ciudad y el departamento de ese punto.
 * Protegido por AuthFilter (requiere sesión de administrador).
 *
 * <ul>
 *   <li>200: punto dentro de un barrio registrado → jerarquía y coordenadas redondeadas.</li>
 *   <li>400 VALIDACION: falta latitud/longitud, no son números o están fuera de rango.</li>
 *   <li>404 FUERA_DE_ZONA: el punto no está dentro de ningún barrio registrado.</li>
 * </ul>
 *
 * <p>Al guardar la propiedad se vuelve a verificar (service + trigger de la base), así que
 * esta consulta es una ayuda para el formulario, no un reemplazo de la validación.</p>
 */
@WebServlet(name = "UbicacionAdminServlet", urlPatterns = "/api/admin/ubicacion")
public class UbicacionAdminServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Errores errores = new Errores();
        var latitud = Parametros.decimal(req, "latitud", errores);
        var longitud = Parametros.decimal(req, "longitud", errores);
        errores.lanzarSiHay(); // formato inválido; la ausencia y el rango los valida el service

        var detectada = servicio(UbicacionService.class).localizar(latitud, longitud);
        escribirJson(resp, HttpServletResponse.SC_OK, CatalogoMapper.ubicacion(detectada));
    }
}

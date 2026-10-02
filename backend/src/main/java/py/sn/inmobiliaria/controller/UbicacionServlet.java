package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.mapper.GeoJsonMapper;
import py.sn.inmobiliaria.service.UbicacionService;
import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.util.Parametros;

import java.io.IOException;

/**
 * Datos geográficos del mapa, en GeoJSON:
 * <ul>
 *   <li>{@code GET /api/ciudades} → ciudades como puntos.</li>
 *   <li>{@code GET /api/barrios} y {@code GET /api/barrios?ciudadId=1} → polígonos de los barrios.</li>
 * </ul>
 * Estos datos casi no cambian, por eso el navegador puede guardarlos una hora.
 */
@WebServlet(name = "UbicacionServlet", urlPatterns = {"/api/ciudades", "/api/barrios"})
public class UbicacionServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final int UNA_HORA = 3600;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UbicacionService ubicaciones = servicio(UbicacionService.class);
        if ("/api/ciudades".equals(req.getServletPath())) {
            var ciudades = GeoJsonMapper.ciudades(ubicaciones.listarCiudades());
            permitirCache(resp, UNA_HORA);
            escribirJson(resp, HttpServletResponse.SC_OK, ciudades);
            return;
        }
        Errores errores = new Errores();
        Integer ciudadId = Parametros.entero(req, "ciudadId", errores);
        errores.lanzarSiHay();
        var barrios = GeoJsonMapper.barrios(ubicaciones.listarBarrios(ciudadId));
        permitirCache(resp, UNA_HORA);
        escribirJson(resp, HttpServletResponse.SC_OK, barrios);
    }
}

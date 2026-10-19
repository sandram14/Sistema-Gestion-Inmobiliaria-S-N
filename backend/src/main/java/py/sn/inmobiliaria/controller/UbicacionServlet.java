package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.mapper.GeoJsonMapper;
import py.sn.inmobiliaria.service.UbicacionService;
import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.util.Parametros;

import java.io.IOException;

/**
 * Datos geográficos del mapa, en GeoJSON:
 * <ul>
 *   <li>{@code GET /api/ciudades} y {@code GET /api/ciudades?departamentoId=1} → ciudades como puntos.</li>
 *   <li>{@code GET /api/barrios} y {@code GET /api/barrios?ciudadId=1} → polígonos de los barrios, simplificados
 *       para dibujar (unos 3 m; la validación de ubicaciones usa el polígono exacto).</li>
 *   <li>{@code conPropiedades=true} (en ambos): solo las ciudades con al menos una propiedad Disponible, y los
 *       barrios de esas ciudades. Lo usa el mapa del sitio, que además muestra solo los barrios donde hay propiedades
 *       (lo calcula con la lista de propiedades, que nunca se guarda); el panel pide todas para cargar en cualquier zona.</li>
 * </ul>
 * La lista completa casi no cambia: el navegador puede guardarla una hora. La de ciudades con propiedades cambia al
 * cargar la primera propiedad de una ciudad: se guarda solo cinco minutos.
 */
@WebServlet(name = "UbicacionServlet", urlPatterns = {"/api/ciudades", "/api/barrios"})
public class UbicacionServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final int UNA_HORA = 3600;
    private static final int CINCO_MINUTOS = 300;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UbicacionService ubicaciones = servicio(UbicacionService.class);
        if ("/api/ciudades".equals(req.getServletPath())) {
            Errores errores = new Errores();
            Integer departamentoId = Parametros.entero(req, "departamentoId", errores);
            boolean conPropiedades = Parametros.booleano(req, "conPropiedades", errores);
            errores.lanzarSiHay();
            if (departamentoId != null && departamentoId <= 0) {
                throw ValidacionException.deCampo("departamentoId", "Debe ser un número positivo");
            }
            var ciudades = GeoJsonMapper.ciudades(ubicaciones.listarCiudades(departamentoId, conPropiedades));
            permitirCache(resp, conPropiedades ? CINCO_MINUTOS : UNA_HORA);
            escribirJson(resp, HttpServletResponse.SC_OK, ciudades);
            return;
        }
        Errores errores = new Errores();
        Integer ciudadId = Parametros.entero(req, "ciudadId", errores);
        boolean conPropiedades = Parametros.booleano(req, "conPropiedades", errores);
        errores.lanzarSiHay();
        var barrios = GeoJsonMapper.barrios(ubicaciones.listarBarrios(ciudadId, conPropiedades));
        permitirCache(resp, conPropiedades ? CINCO_MINUTOS : UNA_HORA);
        escribirJson(resp, HttpServletResponse.SC_OK, barrios);
    }
}

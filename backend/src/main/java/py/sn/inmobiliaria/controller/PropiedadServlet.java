package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.dto.response.geojson.Feature;
import py.sn.inmobiliaria.dto.response.geojson.FeatureCollection;
import py.sn.inmobiliaria.dto.response.geojson.PropiedadProps;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.mapper.GeoJsonMapper;
import py.sn.inmobiliaria.service.PropiedadConsultaService;
import py.sn.inmobiliaria.service.ReglasNegocio;
import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.util.Parametros;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * API pública de propiedades (solo las Disponibles).
 *
 * <ul>
 *   <li>{@code GET /api/propiedades} → FeatureCollection. Filtros opcionales:
 *       departamentoId, ciudadId, barrioId, categoria (Casa | Vivienda | Terreno), operacion (o tipoOperacion),
 *       presupuestoMax, moneda (USD por defecto si hay presupuesto).</li>
 *   <li>{@code GET /api/propiedades/{codigo}} → Feature de una propiedad (por ejemplo prop-001).</li>
 * </ul>
 */
@WebServlet(name = "PropiedadServlet", urlPatterns = {"/api/propiedades", "/api/propiedades/*"})
public class PropiedadServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    /** Formato del código público; cualquier otra cosa es 404 sin consultar la base. */
    private static final Pattern CODIGO = Pattern.compile("^/([a-zA-Z]+-[0-9]{3,})$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ruta = req.getPathInfo();
        if (ruta == null || "/".equals(ruta)) {
            listar(req, resp);
            return;
        }
        var coincidencia = CODIGO.matcher(ruta);
        if (!coincidencia.matches()) {
            throw new NoEncontradoException("La propiedad no existe o ya no está disponible");
        }
        Feature<PropiedadProps> propiedad = servicio(PropiedadConsultaService.class)
                .obtenerPorCodigo(coincidencia.group(1), GeoJsonMapper.propiedad(null));
        escribirJson(resp, HttpServletResponse.SC_OK, propiedad);
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Errores errores = new Errores();
        Integer departamentoId = Parametros.entero(req, "departamentoId", errores);
        Integer ciudadId = Parametros.entero(req, "ciudadId", errores);
        Integer barrioId = Parametros.entero(req, "barrioId", errores);
        var presupuesto = Parametros.decimal(req, "presupuestoMax", errores);
        errores.lanzarSiHay();

        String operacion = Parametros.texto(req, "operacion", "tipoOperacion");
        BusquedaRequest criterios = new BusquedaRequest(ciudadId, barrioId,
                Parametros.texto(req, "categoria"), operacion, presupuesto, Parametros.texto(req, "moneda"),
                departamentoId);

        // Si se filtra por Alquiler, cada propiedad muestra su precio de alquiler
        String operacionPreferida = ReglasNegocio.normalizarOperacion(operacion).orElse(null);
        List<Feature<PropiedadProps>> propiedades = servicio(PropiedadConsultaService.class)
                .buscar(criterios, GeoJsonMapper.propiedad(operacionPreferida));
        escribirJson(resp, HttpServletResponse.SC_OK, FeatureCollection.de(propiedades));
    }
}

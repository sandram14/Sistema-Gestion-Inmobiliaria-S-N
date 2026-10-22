package py.sn.inmobiliaria.mapper;

import py.sn.inmobiliaria.dto.response.geojson.BarrioProps;
import py.sn.inmobiliaria.dto.response.geojson.CiudadProps;
import py.sn.inmobiliaria.dto.response.geojson.Feature;
import py.sn.inmobiliaria.dto.response.geojson.FeatureCollection;
import py.sn.inmobiliaria.dto.response.geojson.GeometriaCruda;
import py.sn.inmobiliaria.dto.response.geojson.PropiedadProps;
import py.sn.inmobiliaria.dto.response.geojson.PropiedadProps.OperacionDto;
import py.sn.inmobiliaria.dto.response.geojson.Punto;
import py.sn.inmobiliaria.model.Casa;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Terreno;
import py.sn.inmobiliaria.repository.UbicacionDao.BarrioGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.CiudadGeo;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * Arma el GeoJSON que consume el mapa del frontend (useGeoData.ts), con los mismos
 * campos que tenían ciudades.geojson, barrios.geojson y propiedades.geojson.
 */
public final class GeoJsonMapper {

    private GeoJsonMapper() {
    }

    // ---- Ciudades y barrios (proyecciones PostGIS) ----

    public static FeatureCollection<CiudadProps> ciudades(List<CiudadGeo> ciudades) {
        return FeatureCollection.de(ciudades.stream()
                .map(c -> Feature.de(Punto.de(c.longitud(), c.latitud()),
                        new CiudadProps(String.valueOf(c.id()), c.nombre(), c.departamento(), c.totalBarrios(),
                                Formato.color(c.id()), c.idDepartamento())))
                .toList());
    }

    public static FeatureCollection<BarrioProps> barrios(List<BarrioGeo> barrios) {
        return FeatureCollection.de(barrios.stream()
                .map(b -> Feature.de(new GeometriaCruda(b.geometriaGeoJson()),
                        new BarrioProps(b.id(), b.nombre(), String.valueOf(b.idCiudad()), b.ciudad(), b.departamento(),
                                Formato.color(b.id()))))
                .toList());
    }

    // ---- Propiedades (entidades) ----

    /**
     * Conversor de una propiedad a Feature GeoJSON, para pasarlo a PropiedadConsultaService.
     *
     * @param operacionPreferida la operación del filtro ("Venta"/"Alquiler") o null: define
     *                           qué precio se muestra en tipoOperacion/precio/moneda
     */
    public static Function<Propiedad, Feature<PropiedadProps>> propiedad(String operacionPreferida) {
        return p -> Feature.de(
                Punto.de(p.getLongitud().doubleValue(), p.getLatitud().doubleValue()),
                propiedadProps(p, operacionPreferida));
    }

    static PropiedadProps propiedadProps(Propiedad p, String operacionPreferida) {
        List<OperacionDto> operaciones = Formato.operaciones(p);
        OperacionDto principal = elegirPrincipal(operaciones, operacionPreferida);

        Casa casa = p.getCasa();
        Terreno terreno = p.getTerreno();
        int dormitorios = casa != null ? casa.getHabitaciones() : 0;
        int banos = casa != null ? casa.getBanos() : 0;
        BigDecimal m2 = casa != null ? casa.getSuperficieConstruida()
                : terreno != null ? terreno.getSuperficie() : null;

        List<String> fotos = p.getImagenes().stream().map(Imagen::getUrl).toList(); // ya vienen ordenadas

        return new PropiedadProps(
                p.getCodigo(),
                p.getTitulo(),
                p.getTipo().getNombre(),
                principal == null ? null : principal.operacion(),
                p.getBarrio().getCiudad().getNombre(),
                p.getBarrio().getNombre(),
                p.getBarrio().getId(),
                principal == null ? null : principal.precio(),
                principal == null ? null : principal.moneda(),
                dormitorios,
                banos,
                Formato.numero(m2),
                p.getDireccion() == null ? "" : p.getDireccion(),
                fotos,
                operaciones,
                Formato.textoONulo(p.getDescripcion()));
    }

    /** La del filtro si existe; si no, Venta; si no, la primera. */
    private static OperacionDto elegirPrincipal(List<OperacionDto> operaciones, String preferida) {
        if (operaciones.isEmpty()) {
            return null;
        }
        for (String buscada : new String[]{preferida, Operacion.VENTA}) {
            if (buscada != null) {
                for (OperacionDto op : operaciones) {
                    if (op.operacion().equals(buscada)) {
                        return op;
                    }
                }
            }
        }
        return operaciones.get(0);
    }
}

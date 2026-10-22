package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.exception.FueraDeZonaException;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;
import py.sn.inmobiliaria.repository.UbicacionDao;
import py.sn.inmobiliaria.repository.UbicacionDao.BarrioGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.CiudadGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.FilaUbicacion;

import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.service.validacion.ValidadorPropiedad;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Datos geográficos para el mapa: ciudades y barrios (consultas PostGIS). */
public class UbicacionService {

    private final UbicacionDao ubicacionDao;

    public UbicacionService(UbicacionDao ubicacionDao) {
        this.ubicacionDao = ubicacionDao;
    }

    /**
     * @param departamentoId     null para todas las ciudades
     * @param soloConPropiedades true: solo las que tienen al menos una propiedad Disponible (mapa del sitio)
     */
    public List<CiudadGeo> listarCiudades(Integer departamentoId, boolean soloConPropiedades) {
        try {
            return ubicacionDao.listarCiudades(departamentoId, soloConPropiedades);
        } catch (SQLException e) {
            throw sinBase(e);
        }
    }

    /** Todas las ciudades de un departamento (null: todas), tengan o no propiedades. */
    public List<CiudadGeo> listarCiudades(Integer departamentoId) {
        return listarCiudades(departamentoId, false);
    }

    /**
     * @param ciudadId           null para todos los barrios
     * @param soloConPropiedades true: solo los barrios de ciudades con alguna propiedad Disponible (mapa del sitio)
     * @throws NoEncontradoException si se indica una ciudad que no existe
     */
    public List<BarrioGeo> listarBarrios(Integer ciudadId, boolean soloConPropiedades) {
        try {
            if (ciudadId != null && ubicacionDao.listarCiudades().stream().noneMatch(c -> c.id() == ciudadId)) {
                throw new NoEncontradoException("La ciudad indicada no existe");
            }
            return ubicacionDao.listarBarrios(ciudadId, soloConPropiedades);
        } catch (SQLException e) {
            throw sinBase(e);
        }
    }

    /** Todos los barrios de una ciudad (null: todos), tenga o no propiedades. */
    public List<BarrioGeo> listarBarrios(Integer ciudadId) {
        return listarBarrios(ciudadId, false);
    }

    /**
     * Detecta el barrio, la ciudad y el departamento de un punto del mapa (para el panel).
     * Las coordenadas se validan y redondean igual que al guardar una propiedad, así la
     * respuesta coincide con lo que después aceptará el trigger de la base.
     *
     * @throws py.sn.inmobiliaria.exception.ValidacionException si faltan o son inválidas (400)
     * @throws FueraDeZonaException                          si el punto no está en ningún barrio (404)
     */
    public UbicacionDetectada localizar(BigDecimal latitud, BigDecimal longitud) {
        Errores errores = new Errores();
        BigDecimal lat = ValidadorPropiedad.latitud(latitud, errores);
        BigDecimal lng = ValidadorPropiedad.longitud(longitud, errores);
        errores.lanzarSiHay();
        try {
            FilaUbicacion fila = ubicacionDao.barrioQueContiene(lat, lng).orElseThrow(FueraDeZonaException::new);
            return new UbicacionDetectada(lat, lng, fila);
        } catch (SQLException e) {
            throw sinBase(e);
        }
    }

    /** Punto (ya redondeado a la precisión de la base) y la jerarquía que lo contiene. */
    public record UbicacionDetectada(BigDecimal latitud, BigDecimal longitud, FilaUbicacion ubicacion) {
    }

    public List<FilaUbicacion> listarJerarquia() {
        try {
            return ubicacionDao.listarJerarquia();
        } catch (SQLException e) {
            throw sinBase(e);
        }
    }

    private static ServicioNoDisponibleException sinBase(SQLException e) {
        return new ServicioNoDisponibleException("No se pudieron obtener los datos geográficos. Intente nuevamente.", e);
    }
}

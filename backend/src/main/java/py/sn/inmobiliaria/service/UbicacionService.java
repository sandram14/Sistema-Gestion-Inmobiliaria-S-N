package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;
import py.sn.inmobiliaria.repository.UbicacionDao;
import py.sn.inmobiliaria.repository.UbicacionDao.BarrioGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.CiudadGeo;
import py.sn.inmobiliaria.repository.UbicacionDao.FilaUbicacion;

import java.sql.SQLException;
import java.util.List;

/** Datos geográficos para el mapa: ciudades y barrios (consultas PostGIS). */
public class UbicacionService {

    private final UbicacionDao ubicacionDao;

    public UbicacionService(UbicacionDao ubicacionDao) {
        this.ubicacionDao = ubicacionDao;
    }

    public List<CiudadGeo> listarCiudades() {
        try {
            return ubicacionDao.listarCiudades();
        } catch (SQLException e) {
            throw sinBase(e);
        }
    }

    /**
     * @param ciudadId null para todos los barrios
     * @throws NoEncontradoException si se indica una ciudad que no existe
     */
    public List<BarrioGeo> listarBarrios(Integer ciudadId) {
        try {
            if (ciudadId != null && ubicacionDao.listarCiudades().stream().noneMatch(c -> c.id() == ciudadId)) {
                throw new NoEncontradoException("La ciudad indicada no existe");
            }
            return ubicacionDao.listarBarrios(ciudadId);
        } catch (SQLException e) {
            throw sinBase(e);
        }
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

package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Barrio;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.model.TipoPropiedad;

import java.util.List;
import java.util.Optional;

/** Acceso a los catálogos (tipos, estados, operaciones, roles) y a barrios por id. */
public interface CatalogoDao {

    List<TipoPropiedad> listarTipos(EntityManager em);

    List<EstadoPropiedad> listarEstados(EntityManager em);

    List<Operacion> listarOperaciones(EntityManager em);

    Optional<TipoPropiedad> tipoPorNombre(EntityManager em, String nombre);

    Optional<EstadoPropiedad> estadoPorNombre(EntityManager em, String nombre);

    Optional<Operacion> operacionPorNombre(EntityManager em, String nombre);

    Optional<Rol> rolPorNombre(EntityManager em, String nombre);

    Optional<Barrio> barrioPorId(EntityManager em, int idBarrio);
}

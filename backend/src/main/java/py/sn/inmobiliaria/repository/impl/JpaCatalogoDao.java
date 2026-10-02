package py.sn.inmobiliaria.repository.impl;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Barrio;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.model.TipoPropiedad;
import py.sn.inmobiliaria.repository.CatalogoDao;

import java.util.List;
import java.util.Optional;

/** Implementación con JPA/Hibernate. */
public class JpaCatalogoDao implements CatalogoDao {

    @Override
    public List<TipoPropiedad> listarTipos(EntityManager em) {
        return em.createQuery("select t from TipoPropiedad t order by t.id", TipoPropiedad.class).getResultList();
    }

    @Override
    public List<EstadoPropiedad> listarEstados(EntityManager em) {
        return em.createQuery("select e from EstadoPropiedad e order by e.id", EstadoPropiedad.class).getResultList();
    }

    @Override
    public List<Operacion> listarOperaciones(EntityManager em) {
        return em.createQuery("select o from Operacion o order by o.id", Operacion.class).getResultList();
    }

    @Override
    public Optional<TipoPropiedad> tipoPorNombre(EntityManager em, String nombre) {
        return porNombre(em, TipoPropiedad.class, "TipoPropiedad", nombre);
    }

    @Override
    public Optional<EstadoPropiedad> estadoPorNombre(EntityManager em, String nombre) {
        return porNombre(em, EstadoPropiedad.class, "EstadoPropiedad", nombre);
    }

    @Override
    public Optional<Operacion> operacionPorNombre(EntityManager em, String nombre) {
        return porNombre(em, Operacion.class, "Operacion", nombre);
    }

    @Override
    public Optional<Rol> rolPorNombre(EntityManager em, String nombre) {
        return porNombre(em, Rol.class, "Rol", nombre);
    }

    @Override
    public Optional<Barrio> barrioPorId(EntityManager em, int idBarrio) {
        return Optional.ofNullable(em.find(Barrio.class, idBarrio));
    }

    /** El nombre de la entidad es una constante del código, nunca un dato del usuario. */
    private static <T> Optional<T> porNombre(EntityManager em, Class<T> clase, String entidad, String nombre) {
        return em.createQuery("select x from " + entidad + " x where x.nombre = :nombre", clase)
                .setParameter("nombre", nombre)
                .getResultStream()
                .findFirst();
    }
}

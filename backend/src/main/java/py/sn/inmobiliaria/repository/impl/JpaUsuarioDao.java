package py.sn.inmobiliaria.repository.impl;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.repository.UsuarioDao;

import java.util.Optional;

/** Implementación con JPA/Hibernate. */
public class JpaUsuarioDao implements UsuarioDao {

    @Override
    public Optional<Usuario> buscarActivoPorEmail(EntityManager em, String email) {
        return em.createQuery("""
                        select u from Usuario u join fetch u.rol
                         where lower(u.email) = lower(:email) and u.activo = true
                        """, Usuario.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(EntityManager em, int id) {
        return em.createQuery("select u from Usuario u join fetch u.rol where u.id = :id", Usuario.class)
                .setParameter("id", id).getResultStream().findFirst();
    }

    @Override
    public java.util.List<Usuario> listar(EntityManager em) {
        return em.createQuery("select u from Usuario u join fetch u.rol order by lower(u.apellido), lower(u.nombre)", Usuario.class)
                .getResultList();
    }

    @Override
    public boolean existeEmail(EntityManager em, String email, Integer excluirId) {
        return em.createQuery("select count(u) from Usuario u where lower(u.email) = lower(:e) and (:x is null or u.id <> :x)", Long.class)
                .setParameter("e", email).setParameter("x", excluirId).getSingleResult() > 0;
    }

    @Override
    public boolean existeCedula(EntityManager em, String cedula, Integer excluirId) {
        return em.createQuery("select count(u) from Usuario u where u.cedula = :c and (:x is null or u.id <> :x)", Long.class)
                .setParameter("c", cedula).setParameter("x", excluirId).getSingleResult() > 0;
    }

    @Override
    public long contarAdministradoresActivos(EntityManager em) {
        return em.createQuery("select count(u) from Usuario u where u.activo = true and u.rol.nombre = 'ADMIN'", Long.class)
                .getSingleResult();
    }

    @Override
    public java.util.Map<Integer, Long> propiedadesPorResponsable(EntityManager em) {
        java.util.Map<Integer, Long> mapa = new java.util.HashMap<>();
        for (Object[] fila : em.createQuery(
                "select p.responsable.id, count(p) from Propiedad p where p.responsable is not null group by p.responsable.id",
                Object[].class).getResultList()) {
            mapa.put((Integer) fila[0], (Long) fila[1]);
        }
        return mapa;
    }

    @Override
    public void guardar(EntityManager em, Usuario usuario) {
        em.persist(usuario);
    }

    @Override
    public Optional<Usuario> buscarActivoPorId(EntityManager em, int id) {
        return em.createQuery("select u from Usuario u join fetch u.rol where u.id = :id and u.activo = true", Usuario.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }
}

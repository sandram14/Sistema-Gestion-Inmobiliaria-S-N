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
}

package py.sn.inmobiliaria.repository.impl;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.repository.ImagenDao;

import java.util.List;
import java.util.Optional;

/** Implementación con JPA/Hibernate. */
public class JpaImagenDao implements ImagenDao {

    @Override
    public Optional<Imagen> buscar(EntityManager em, long idPropiedad, long idImagen) {
        return em.createQuery(
                        "select i from Imagen i where i.id = :idImagen and i.propiedad.id = :idPropiedad", Imagen.class)
                .setParameter("idImagen", idImagen)
                .setParameter("idPropiedad", idPropiedad)
                .getResultStream()
                .findFirst();
    }

    @Override
    public int desmarcarPrincipal(EntityManager em, long idPropiedad) {
        List<Imagen> principales = em.createQuery(
                        "select i from Imagen i where i.propiedad.id = :id and i.esPrincipal = true", Imagen.class)
                .setParameter("id", idPropiedad)
                .getResultList();
        principales.forEach(i -> i.setEsPrincipal(false));
        // flush: el UPDATE a false llega a la base ahora, antes de que se marque la nueva
        // principal; así nunca hay dos principales a la vez (índice uq_imagen_principal).
        em.flush();
        return principales.size();
    }
}

package py.sn.inmobiliaria.repository.impl;

import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.repository.PropiedadDao;

/** Implementación con JPA/Hibernate (consultas JPQL parametrizadas). */
public class JpaPropiedadDao implements PropiedadDao {

    private final TxRunner tx;

    public JpaPropiedadDao(TxRunner tx) {
        this.tx = tx;
    }

    @Override
    public long contarDisponibles() {
        return tx.soloLectura(em -> em.createQuery(
                        "select count(p) from Propiedad p where p.estado.nombre = :estado", Long.class)
                .setParameter("estado", EstadoPropiedad.DISPONIBLE)
                .getSingleResult());
    }
}

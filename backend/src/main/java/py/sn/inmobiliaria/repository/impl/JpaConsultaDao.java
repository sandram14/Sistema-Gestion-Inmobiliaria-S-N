package py.sn.inmobiliaria.repository.impl;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Consulta;
import py.sn.inmobiliaria.repository.ConsultaDao;

/** Implementación con JPA/Hibernate. */
public class JpaConsultaDao implements ConsultaDao {

    @Override
    public void guardar(EntityManager em, Consulta consulta) {
        em.persist(consulta);
    }
}

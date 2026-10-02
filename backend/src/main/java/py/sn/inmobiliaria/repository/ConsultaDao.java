package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Consulta;

/** Registro de consultas al asistente de IA (Fase 14). */
public interface ConsultaDao {

    void guardar(EntityManager em, Consulta consulta);
}

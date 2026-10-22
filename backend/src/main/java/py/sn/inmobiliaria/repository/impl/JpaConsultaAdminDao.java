package py.sn.inmobiliaria.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.sn.inmobiliaria.model.Consulta;
import py.sn.inmobiliaria.repository.ConsultaAdminDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class JpaConsultaAdminDao implements ConsultaAdminDao {

    // Uniones EXTERNAS: una consulta general (sin propiedad) no debe quedar excluida por la unión
    private static final String BASE = "select distinct c from Consulta c left join fetch c.propiedad p"
            + " left join p.responsable pr left join fetch c.revisadaPor where 1 = 1";

    // Visibilidad del vendedor, aplicada en la consulta a la base (no se filtra después en memoria)
    private static final String VISIBLE = " and (p is null or pr.id = :vendedor"
            + " or exists (select 1 from Consulta c2 join c2.recomendadas r join r.responsable rr where c2 = c and rr.id = :vendedor))";

    /** Condiciones y parámetros del filtro (comunes a la búsqueda y al conteo). */
    private static String condiciones(Filtro f, Map<String, Object> params) {
        StringBuilder w = new StringBuilder();
        if (f.texto() != null) {
            w.append(" and (lower(c.consulta) like :texto or lower(coalesce(c.respuesta, '')) like :texto)");
            params.put("texto", "%" + f.texto().toLowerCase(java.util.Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (f.desde() != null) { w.append(" and c.fechaHora >= :desde"); params.put("desde", f.desde()); }
        if (f.hasta() != null) { w.append(" and c.fechaHora < :hasta"); params.put("hasta", f.hasta()); }
        if (f.estado() != null) { w.append(" and c.estadoRevision = :estado"); params.put("estado", f.estado()); }
        if (f.resultado() != null) {
            if ("NO_REGISTRADO".equals(f.resultado())) {
                w.append(" and c.resultado is null");
            } else {
                w.append(" and c.resultado = :resultado");
                params.put("resultado", f.resultado());
            }
        }
        if (f.idVendedor() != null) { w.append(VISIBLE); params.put("vendedor", f.idVendedor()); }
        return w.toString();
    }

    @Override
    public List<Consulta> buscar(EntityManager em, Filtro f, int desde) {
        Map<String, Object> params = new HashMap<>();
        TypedQuery<Consulta> q = em.createQuery(BASE + condiciones(f, params) + " order by c.fechaHora desc, c.id desc", Consulta.class)
                .setFirstResult(desde).setMaxResults(f.limite());
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    @Override
    public long contar(EntityManager em, Filtro f) {
        Map<String, Object> params = new HashMap<>();
        TypedQuery<Long> q = em.createQuery("select count(distinct c) from Consulta c left join c.propiedad p left join p.responsable pr where 1 = 1"
                + condiciones(f, params), Long.class);
        params.forEach(q::setParameter);
        return q.getSingleResult();
    }

    @Override
    public Optional<Consulta> porId(EntityManager em, long id, Integer idVendedor) {
        TypedQuery<Consulta> q = em.createQuery(BASE + " and c.id = :id" + (idVendedor == null ? "" : VISIBLE), Consulta.class)
                .setParameter("id", id);
        if (idVendedor != null) {
            q.setParameter("vendedor", idVendedor);
        }
        return q.getResultStream().findFirst();
    }

    @Override
    public List<Consulta> deSesion(EntityManager em, UUID sesion, Integer idVendedor) {
        TypedQuery<Consulta> q = em.createQuery(BASE + " and c.idSesion = :s" + (idVendedor == null ? "" : VISIBLE)
                + " order by c.fechaHora", Consulta.class).setParameter("s", sesion);
        if (idVendedor != null) {
            q.setParameter("vendedor", idVendedor);
        }
        return q.getResultList();
    }
}

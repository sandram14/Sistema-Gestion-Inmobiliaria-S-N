package py.sn.inmobiliaria.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.repository.FiltroPropiedades;
import py.sn.inmobiliaria.repository.PropiedadDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación con JPA/Hibernate.
 *
 * <p>Plan de carga para evitar el problema "N+1" (una consulta extra por cada propiedad):</p>
 * <ul>
 *   <li>tipo, estado, barrio, ciudad, casa y terreno se traen en la misma consulta
 *       ({@code join fetch});</li>
 *   <li>imágenes y operaciones se cargan en lotes gracias a
 *       {@code hibernate.default_batch_fetch_size} (persistence.xml).</li>
 * </ul>
 */
public class JpaPropiedadDao implements PropiedadDao {

    /** Parte común de las consultas: la propiedad con todo lo que el frontend muestra. */
    private static final String SELECT_CON_RELACIONES = """
            select p from Propiedad p
              join fetch p.tipo
              join fetch p.estado
              join fetch p.barrio b
              join fetch b.ciudad c
              join fetch c.departamento
              left join fetch p.casa
              left join fetch p.terreno
            """;

    @Override
    public List<Propiedad> buscarDisponibles(EntityManager em, FiltroPropiedades filtro) {
        StringBuilder jpql = new StringBuilder(SELECT_CON_RELACIONES)
                .append(" where p.estado.nombre = :disponible");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("disponible", EstadoPropiedad.DISPONIBLE);

        if (filtro.departamentoId() != null) {
            jpql.append(" and p.barrio.ciudad.departamento.id = :departamentoId");
            parametros.put("departamentoId", filtro.departamentoId());
        }
        if (filtro.ciudadId() != null) {
            jpql.append(" and p.barrio.ciudad.id = :ciudadId");
            parametros.put("ciudadId", filtro.ciudadId());
        }
        if (filtro.barrioId() != null) {
            jpql.append(" and p.barrio.id = :barrioId");
            parametros.put("barrioId", filtro.barrioId());
        }
        if (filtro.tipo() != null) {
            jpql.append(" and p.tipo.nombre = :tipo");
            parametros.put("tipo", filtro.tipo());
        }
        // Operación y presupuesto se evalúan sobre la MISMA fila de operacion_propiedad:
        // "tiene Alquiler a menos de X en PYG", no "tiene Alquiler y algún precio menor a X".
        if (filtro.operacion() != null || filtro.tienePresupuesto()) {
            jpql.append(" and exists (select op from OperacionPropiedad op where op.propiedad = p");
            if (filtro.operacion() != null) {
                jpql.append(" and op.operacion.nombre = :operacion");
                parametros.put("operacion", filtro.operacion());
            }
            if (filtro.tienePresupuesto()) {
                // Sin conversión: se comparan solo los precios publicados en la moneda elegida
                jpql.append(" and op.moneda = :moneda");
                parametros.put("moneda", filtro.moneda());
                if (filtro.presupuestoMax() != null) {
                    jpql.append(" and op.precio <= :presupuestoMax");
                    parametros.put("presupuestoMax", filtro.presupuestoMax());
                }
                if (filtro.presupuestoMin() != null) {
                    jpql.append(" and op.precio >= :presupuestoMin");
                    parametros.put("presupuestoMin", filtro.presupuestoMin());
                }
            }
            jpql.append(")");
        }
        jpql.append(" order by p.codigo");

        TypedQuery<Propiedad> query = em.createQuery(jpql.toString(), Propiedad.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public Optional<Propiedad> buscarDisponiblePorCodigo(EntityManager em, String codigo) {
        return em.createQuery(SELECT_CON_RELACIONES
                                + " where p.codigo = :codigo and p.estado.nombre = :disponible",
                        Propiedad.class)
                .setParameter("codigo", codigo)
                .setParameter("disponible", EstadoPropiedad.DISPONIBLE)
                .getResultStream()
                .findFirst();
    }

    @Override
    public long contarDisponibles(EntityManager em) {
        return em.createQuery(
                        "select count(p) from Propiedad p where p.estado.nombre = :disponible", Long.class)
                .setParameter("disponible", EstadoPropiedad.DISPONIBLE)
                .getSingleResult();
    }

    @Override
    public List<Propiedad> listarTodas(EntityManager em) {
        return em.createQuery(SELECT_CON_RELACIONES + " order by p.codigo", Propiedad.class)
                .getResultList();
    }

    @Override
    public Optional<Propiedad> buscarPorId(EntityManager em, long id) {
        return em.createQuery(SELECT_CON_RELACIONES + " where p.id = :id", Propiedad.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existeCodigo(EntityManager em, String codigo) {
        return em.createQuery("select count(p) from Propiedad p where p.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult() > 0;
    }

    @Override
    public int ultimoNumeroDeCodigo(EntityManager em, String prefijo) {
        // SQL nativo: extraer el número final del código es más claro con una expresión regular
        Object resultado = em.createNativeQuery("""
                        SELECT COALESCE(MAX(CAST(substring(codigo FROM '[0-9]+$') AS integer)), 0)
                          FROM propiedad
                         WHERE codigo LIKE ?1
                        """)
                .setParameter(1, prefijo + "-%")
                .getSingleResult();
        return ((Number) resultado).intValue();
    }

    @Override
    public void guardar(EntityManager em, Propiedad propiedad) {
        em.persist(propiedad);
    }
}

package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Consulta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas del chat IA para el panel (historial y análisis). */
public interface ConsultaAdminDao {

    /**
     * @param texto        palabras a buscar en la pregunta o la respuesta (null: sin filtro)
     * @param idVendedor   si no es null, solo las visibles para ese vendedor: generales (sin propiedad
     *                     consultada) y las de sus propiedades, consultadas o recomendadas
     */
    record Filtro(String texto, OffsetDateTime desde, OffsetDateTime hasta, String estado, String resultado,
                  Integer idVendedor, int limite) {
    }

    /** Las consultas del filtro, de la más reciente a la más antigua, desde la posición "desde". */
    List<Consulta> buscar(EntityManager em, Filtro filtro, int desde);

    default List<Consulta> buscar(EntityManager em, Filtro filtro) {
        return buscar(em, filtro, 0);
    }

    /** Cantidad total de consultas que cumplen el filtro (para paginar). */
    long contar(EntityManager em, Filtro filtro);

    Optional<Consulta> porId(EntityManager em, long id, Integer idVendedor);

    /** Las preguntas de la misma conversación (sesión), en orden, con la misma regla de visibilidad. */
    List<Consulta> deSesion(EntityManager em, UUID sesion, Integer idVendedor);
}

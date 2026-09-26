package py.sn.inmobiliaria.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;

/**
 * Ejecuta trabajo de base de datos dentro de una transacción, sin Spring.
 *
 * <p>Cada llamada abre su propio {@link EntityManager} (como un banco de trabajo
 * individual), inicia la transacción, ejecuta el trabajo y:</p>
 * <ul>
 *   <li>si todo sale bien, confirma (commit);</li>
 *   <li>si ocurre cualquier error, deshace todo (rollback): o se guarda todo, o nada.</li>
 * </ul>
 *
 * <p>Importante: las relaciones LAZY solo pueden recorrerse dentro de la función,
 * mientras el EntityManager está abierto. Por eso los services arman los DTOs ahí dentro.</p>
 */
public class TxRunner {

    private final JpaProvider jpa;

    public TxRunner(JpaProvider jpa) {
        this.jpa = jpa;
    }

    /** Ejecuta {@code trabajo} en una transacción que se confirma si no hay errores. */
    public <T> T enTransaccion(Function<EntityManager, T> trabajo) {
        return ejecutar(trabajo, true);
    }

    /**
     * Ejecuta {@code trabajo} solo para leer: la transacción se deshace siempre al final,
     * así ningún cambio accidental queda guardado.
     */
    public <T> T soloLectura(Function<EntityManager, T> trabajo) {
        return ejecutar(trabajo, false);
    }

    private <T> T ejecutar(Function<EntityManager, T> trabajo, boolean confirmar) {
        EntityManager em = jpa.emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T resultado = trabajo.apply(em);
            if (confirmar) {
                tx.commit();
            } else {
                tx.rollback();
            }
            return resultado;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}

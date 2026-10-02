package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Propiedad;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a propiedades (con sus datos de casa/terreno, operaciones e imágenes).
 *
 * <p>Las propiedades devueltas quedan "conectadas" al EntityManager recibido: sus
 * relaciones pueden recorrerse mientras la transacción siga abierta.</p>
 */
public interface PropiedadDao {

    // ---- Parte pública (solo estado Disponible) ----

    /** Propiedades disponibles que cumplen el filtro, ordenadas por código. */
    List<Propiedad> buscarDisponibles(EntityManager em, FiltroPropiedades filtro);

    /** Una propiedad disponible por su código público (prop-001). */
    Optional<Propiedad> buscarDisponiblePorCodigo(EntityManager em, String codigo);

    /** Cantidad de propiedades disponibles. */
    long contarDisponibles(EntityManager em);

    // ---- Panel administrativo (todos los estados) ----

    /** Todas las propiedades, incluidas las inactivas, ordenadas por código. */
    List<Propiedad> listarTodas(EntityManager em);

    Optional<Propiedad> buscarPorId(EntityManager em, long id);

    boolean existeCodigo(EntityManager em, String codigo);

    /**
     * Mayor número usado con un prefijo: para "prop" devuelve 16 si existe prop-016.
     * Sirve para generar el siguiente código (prop-017). Devuelve 0 si no hay ninguno.
     */
    int ultimoNumeroDeCodigo(EntityManager em, String prefijo);

    /** Guarda una propiedad nueva junto con su casa/terreno, operaciones e imágenes (en cascada). */
    void guardar(EntityManager em, Propiedad propiedad);
}

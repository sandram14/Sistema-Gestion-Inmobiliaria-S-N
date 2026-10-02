package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Imagen;

import java.util.Optional;

/** Operaciones puntuales sobre imágenes (el alta se hace a través de Propiedad). */
public interface ImagenDao {

    /** Una imagen, solo si pertenece a la propiedad indicada. */
    Optional<Imagen> buscar(EntityManager em, long idPropiedad, long idImagen);

    /**
     * Quita la marca de principal a todas las imágenes de la propiedad.
     * Se ejecuta ANTES de marcar la nueva principal, para no chocar con el índice
     * único parcial uq_imagen_principal.
     *
     * @return cantidad de imágenes modificadas
     */
    int desmarcarPrincipal(EntityManager em, long idPropiedad);
}

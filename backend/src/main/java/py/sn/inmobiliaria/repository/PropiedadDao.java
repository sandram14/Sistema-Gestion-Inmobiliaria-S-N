package py.sn.inmobiliaria.repository;

/**
 * Acceso a datos de propiedades.
 * En la Fase 6 se agregan las búsquedas con filtros y el CRUD administrativo.
 */
public interface PropiedadDao {

    /** Cantidad de propiedades en estado Disponible (las que ve el público). */
    long contarDisponibles();
}

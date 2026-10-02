package py.sn.inmobiliaria.service;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.config.TxRunner;

import java.util.function.Function;

/**
 * Base de los services que usan JPA: ejecuta el trabajo en una transacción y traduce
 * los errores de base de datos a errores de la API.
 */
abstract class ServicioBase {

    protected final TxRunner tx;

    protected ServicioBase(TxRunner tx) {
        this.tx = tx;
    }

    /** Lectura: nunca guarda cambios. */
    protected <T> T leer(Function<EntityManager, T> trabajo) {
        try {
            return tx.soloLectura(trabajo);
        } catch (RuntimeException e) {
            throw TraductorErroresBd.traducir(e);
        }
    }

    /** Escritura: todo o nada. */
    protected <T> T escribir(Function<EntityManager, T> trabajo) {
        try {
            return tx.enTransaccion(trabajo);
        } catch (RuntimeException e) {
            throw TraductorErroresBd.traducir(e);
        }
    }
}

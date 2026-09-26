package py.sn.inmobiliaria.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Crea y guarda el {@link EntityManagerFactory} de Hibernate.
 *
 * <p>El EntityManagerFactory es la "fábrica" costosa: se crea una sola vez, usando el
 * mismo pool de conexiones (HikariCP) que el resto del backend. Al crearse, Hibernate
 * valida que las entidades coincidan con las tablas.</p>
 *
 * <p>Se crea bajo demanda: si la base no está disponible al arrancar Tomcat, la
 * aplicación inicia igual y reintenta en el siguiente uso (por ejemplo, /api/health).</p>
 */
public final class JpaProvider implements AutoCloseable {

    public static final String UNIDAD_PERSISTENCIA = "sn-inmobiliaria";

    private final DataSource dataSource;
    private volatile EntityManagerFactory emf;

    public JpaProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Devuelve el EntityManagerFactory, creándolo si todavía no existe.
     *
     * @throws jakarta.persistence.PersistenceException si la base no responde o el esquema no coincide
     */
    public EntityManagerFactory emf() {
        EntityManagerFactory actual = emf;
        if (actual == null) {
            synchronized (this) {
                actual = emf;
                if (actual == null) {
                    actual = Persistence.createEntityManagerFactory(UNIDAD_PERSISTENCIA,
                            Map.of("jakarta.persistence.nonJtaDataSource", dataSource));
                    emf = actual;
                }
            }
        }
        return actual;
    }

    @Override
    public synchronized void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        emf = null;
    }
}

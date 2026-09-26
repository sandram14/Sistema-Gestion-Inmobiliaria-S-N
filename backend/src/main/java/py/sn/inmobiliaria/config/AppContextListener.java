package py.sn.inmobiliaria.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.repository.PropiedadDao;
import py.sn.inmobiliaria.repository.SaludBaseDatosDao;
import py.sn.inmobiliaria.repository.impl.JdbcSaludBaseDatosDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.service.SaludService;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Punto de arranque de la aplicación ("raíz de composición").
 *
 * <p>Tomcat lo ejecuta una sola vez al desplegar el WAR. Aquí se construyen
 * los objetos compartidos (configuración, pool de conexiones, Hibernate, DAOs y
 * services) y se guardan en el ServletContext para que los Servlets los obtengan
 * en su {@code init()}. Es el reemplazo manual de lo que Spring haría automáticamente.</p>
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    private HikariDataSource dataSource;
    private JpaProvider jpa;

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        ServletContext contexto = evento.getServletContext();
        AppConfig config = AppConfig.cargar();
        contexto.setAttribute(AppConfig.ATRIBUTO_CONTEXTO, config);

        log.info("Iniciando {} v{} (entorno: {})", config.nombre(), config.version(), config.entorno());

        SaludBaseDatosDao saludDao = null;
        PropiedadDao propiedadDao = null;

        if (config.baseDatos().isPresent()) {
            BaseDatosConfig bd = config.baseDatos().get();
            dataSource = DataSourceFactory.crear(bd);
            log.info("Pool de conexiones creado para {}", bd.url());

            jpa = new JpaProvider(dataSource);
            TxRunner tx = new TxRunner(jpa);
            contexto.setAttribute(TxRunner.class.getName(), tx);

            saludDao = new JdbcSaludBaseDatosDao(dataSource);
            propiedadDao = new JpaPropiedadDao(tx);

            iniciarHibernate();
        } else {
            log.warn("Base de datos NO configurada: defina DB_URL, DB_USER y DB_PASSWORD");
        }

        contexto.setAttribute(SaludService.class.getName(), new SaludService(config, saludDao, propiedadDao));
    }

    /**
     * Intenta crear Hibernate al arrancar para que un error de mapeo aparezca de inmediato
     * en el log. Si falla (base apagada o esquema distinto), la aplicación sigue
     * iniciando y JpaProvider reintentará en el siguiente uso.
     */
    private void iniciarHibernate() {
        try {
            jpa.emf();
            log.info("Hibernate iniciado: el esquema de la base coincide con las entidades");
        } catch (RuntimeException e) {
            log.error("No se pudo iniciar Hibernate. Si el mensaje menciona 'Schema-validation', "
                    + "una entidad no coincide con las tablas de /database", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        log.info("Deteniendo la aplicación");
        if (jpa != null) {
            jpa.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
        desregistrarDriversJdbc();
    }

    /**
     * Quita del DriverManager los drivers cargados por esta aplicación, para que
     * Tomcat pueda liberar la memoria al redesplegar (evita la advertencia de "memory leak").
     */
    private void desregistrarDriversJdbc() {
        ClassLoader cargadorApp = Thread.currentThread().getContextClassLoader();
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getClassLoader() == cargadorApp) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    log.warn("No se pudo desregistrar el driver {}", driver, e);
                }
            }
        }
    }
}

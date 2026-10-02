package py.sn.inmobiliaria.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.repository.CatalogoDao;
import py.sn.inmobiliaria.repository.ConsultaDao;
import py.sn.inmobiliaria.repository.ImagenDao;
import py.sn.inmobiliaria.repository.PropiedadDao;
import py.sn.inmobiliaria.repository.SaludBaseDatosDao;
import py.sn.inmobiliaria.repository.UbicacionDao;
import py.sn.inmobiliaria.repository.UsuarioDao;
import py.sn.inmobiliaria.repository.impl.JdbcSaludBaseDatosDao;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaConsultaDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.repository.impl.JpaUsuarioDao;
import py.sn.inmobiliaria.service.SaludService;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.function.LongSupplier;

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
        LongSupplier propiedadesDisponibles = null;

        if (config.baseDatos().isPresent()) {
            BaseDatosConfig bd = config.baseDatos().get();
            dataSource = DataSourceFactory.crear(bd);
            log.info("Pool de conexiones creado para {}", bd.url());

            jpa = new JpaProvider(dataSource);
            TxRunner tx = new TxRunner(jpa);
            contexto.setAttribute(TxRunner.class.getName(), tx);

            // DAOs: se crean una sola vez y se comparten (no guardan estado entre peticiones).
            // Los services de la Fase 7 los recibirán desde aquí.
            saludDao = new JdbcSaludBaseDatosDao(dataSource);
            PropiedadDao propiedadDao = new JpaPropiedadDao();
            registrar(contexto, PropiedadDao.class, propiedadDao);
            registrar(contexto, CatalogoDao.class, new JpaCatalogoDao());
            registrar(contexto, ImagenDao.class, new JpaImagenDao());
            registrar(contexto, UsuarioDao.class, new JpaUsuarioDao());
            registrar(contexto, ConsultaDao.class, new JpaConsultaDao());
            registrar(contexto, UbicacionDao.class, new JdbcUbicacionDao(dataSource));

            propiedadesDisponibles = () -> tx.soloLectura(propiedadDao::contarDisponibles);

            iniciarHibernate();
        } else {
            log.warn("Base de datos NO configurada: defina DB_URL, DB_USER y DB_PASSWORD");
        }

        contexto.setAttribute(SaludService.class.getName(), new SaludService(config, saludDao, propiedadesDisponibles));
    }

    /** Guarda un objeto compartido en el ServletContext, usando el nombre de su tipo como clave. */
    private static <T> void registrar(ServletContext contexto, Class<T> tipo, T instancia) {
        contexto.setAttribute(tipo.getName(), instancia);
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

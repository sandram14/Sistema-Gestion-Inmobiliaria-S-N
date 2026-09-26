package py.sn.inmobiliaria.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de arranque de la aplicación ("raíz de composición").
 *
 * <p>Tomcat lo ejecuta una sola vez al desplegar el WAR. Aquí se construyen
 * los objetos compartidos (configuración y, en fases siguientes,
 * EntityManagerFactory, DAOs y services) y se guardan en el ServletContext
 * para que los Servlets los obtengan en su {@code init()}.</p>
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        ServletContext contexto = evento.getServletContext();
        AppConfig config = AppConfig.cargar();
        contexto.setAttribute(AppConfig.ATRIBUTO_CONTEXTO, config);

        log.info("Iniciando {} v{} (entorno: {})", config.nombre(), config.version(), config.entorno());
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        log.info("Deteniendo la aplicación");
        // En fases siguientes: cerrar el EntityManagerFactory y el pool de conexiones.
    }
}

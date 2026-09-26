package py.sn.inmobiliaria.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuración de la aplicación.
 *
 * <p>Los valores que cambian entre entornos (desarrollo, Docker, producción)
 * y los secretos se leen de variables de entorno; nunca se escriben en el código.
 * Los datos fijos del artefacto (nombre, versión) se leen de {@code app.properties},
 * que Maven completa al compilar.</p>
 */
public final class AppConfig {

    /** Nombre del atributo con el que se guarda en el ServletContext. */
    public static final String ATRIBUTO_CONTEXTO = AppConfig.class.getName();

    private final String nombre;
    private final String version;
    private final String entorno;

    private AppConfig(String nombre, String version, String entorno) {
        this.nombre = nombre;
        this.version = version;
        this.entorno = entorno;
    }

    /** Crea la configuración real: variables de entorno del sistema + app.properties. */
    public static AppConfig cargar() {
        return desde(System.getenv(), leerPropiedades());
    }

    /**
     * Crea la configuración a partir de valores explícitos.
     * Permite probarla sin depender de las variables de entorno de la máquina.
     */
    static AppConfig desde(Map<String, String> entorno, Properties propiedades) {
        Objects.requireNonNull(entorno, "entorno");
        Objects.requireNonNull(propiedades, "propiedades");
        String nombre = propiedades.getProperty("app.nombre", "sn-inmobiliaria-backend");
        String version = propiedades.getProperty("app.version", "desconocida");
        String appEnv = valorOPorDefecto(entorno.get("APP_ENV"), "desarrollo");
        return new AppConfig(nombre, version, appEnv);
    }

    private static Properties leerPropiedades() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // Sin app.properties se usan los valores por defecto; no es un error fatal.
        }
        return props;
    }

    private static String valorOPorDefecto(String valor, String porDefecto) {
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }

    public String nombre() {
        return nombre;
    }

    public String version() {
        return version;
    }

    public String entorno() {
        return entorno;
    }

    public boolean esProduccion() {
        return "produccion".equalsIgnoreCase(entorno);
    }
}

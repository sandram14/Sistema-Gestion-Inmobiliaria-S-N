package py.sn.inmobiliaria.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuración de la aplicación.
 *
 * <p>Los valores que cambian entre entornos (desarrollo, Docker, producción)
 * y los secretos se leen de variables de entorno; nunca se escriben en el código.
 * Los datos fijos del artefacto (nombre, versión) se leen de {@code app.properties},
 * que Maven completa al compilar.</p>
 *
 * <p>Variables reconocidas:</p>
 * <ul>
 *   <li>{@code APP_ENV}: desarrollo | produccion (por defecto: desarrollo)</li>
 *   <li>{@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}: conexión a PostgreSQL.
 *       Si falta alguna, la aplicación arranca igual y /api/health lo informa.</li>
 * </ul>
 */
public final class AppConfig {

    /** Nombre del atributo con el que se guarda en el ServletContext. */
    public static final String ATRIBUTO_CONTEXTO = AppConfig.class.getName();

    private final String nombre;
    private final String version;
    private final String entorno;
    private final BaseDatosConfig baseDatos; // null si no está configurada

    private AppConfig(String nombre, String version, String entorno, BaseDatosConfig baseDatos) {
        this.nombre = nombre;
        this.version = version;
        this.entorno = entorno;
        this.baseDatos = baseDatos;
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
        return new AppConfig(nombre, version, appEnv, leerBaseDatos(entorno));
    }

    private static BaseDatosConfig leerBaseDatos(Map<String, String> entorno) {
        String url = limpio(entorno.get("DB_URL"));
        String usuario = limpio(entorno.get("DB_USER"));
        String contrasena = entorno.get("DB_PASSWORD"); // la contraseña no se recorta
        if (url == null || usuario == null || contrasena == null || contrasena.isEmpty()) {
            return null;
        }
        return new BaseDatosConfig(url, usuario, contrasena);
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

    private static String limpio(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static String valorOPorDefecto(String valor, String porDefecto) {
        String v = limpio(valor);
        return v == null ? porDefecto : v;
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

    /** Conexión a la base de datos, si las tres variables DB_* están definidas. */
    public Optional<BaseDatosConfig> baseDatos() {
        return Optional.ofNullable(baseDatos);
    }
}

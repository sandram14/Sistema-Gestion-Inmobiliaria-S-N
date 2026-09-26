package py.sn.inmobiliaria.config;

/**
 * Datos de conexión a PostgreSQL, leídos de las variables de entorno
 * DB_URL, DB_USER y DB_PASSWORD.
 *
 * <p>{@link #toString()} oculta la contraseña para que nunca aparezca en los logs.</p>
 */
public record BaseDatosConfig(String url, String usuario, String contrasena) {

    @Override
    public String toString() {
        return "BaseDatosConfig[url=" + url + ", usuario=" + usuario + ", contrasena=****]";
    }
}

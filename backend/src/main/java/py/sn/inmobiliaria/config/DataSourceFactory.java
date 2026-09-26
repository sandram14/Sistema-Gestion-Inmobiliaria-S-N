package py.sn.inmobiliaria.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Crea el pool de conexiones a PostgreSQL (HikariCP).
 *
 * <p>Un pool es como una fila de taxis esperando: en lugar de abrir una conexión
 * nueva (lento) en cada petición, se toma una ya abierta y se devuelve al terminar.
 * En la Fase 5, Hibernate utilizará este mismo pool.</p>
 */
public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static HikariDataSource crear(BaseDatosConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("sn-pool");
        hikari.setDriverClassName("org.postgresql.Driver");
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.usuario());
        hikari.setPassword(config.contrasena());
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(5_000);           // ms esperando una conexión libre
        // -1: la aplicación arranca aunque la base todavía no esté disponible
        // (por ejemplo, si el contenedor de PostgreSQL tarda en iniciar).
        hikari.setInitializationFailTimeout(-1);
        hikari.addDataSourceProperty("ApplicationName", "sn-inmobiliaria-backend");
        return new HikariDataSource(hikari);
    }
}

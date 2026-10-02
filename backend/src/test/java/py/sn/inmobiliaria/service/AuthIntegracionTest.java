package py.sn.inmobiliaria.service;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.exception.NoAutenticadoException;
import py.sn.inmobiliaria.repository.impl.JpaUsuarioDao;
import py.sn.inmobiliaria.seguridad.ContrasenaHasher;
import py.sn.inmobiliaria.seguridad.LimitadorIntentos;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Login con el administrador REAL creado por database/seed/04_admin_dev.sh (hash BCrypt
 * generado por PostgreSQL). Necesita DB_URL y además ADMIN_EMAIL y ADMIN_PASSWORD
 * (los mismos valores de docker/.env).
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ADMIN_PASSWORD", matches = ".+")
class AuthIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static AuthService auth;

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        auth = new AuthService(new TxRunner(jpa), new JpaUsuarioDao(), new ContrasenaHasher(), new LimitadorIntentos());
    }

    @AfterAll
    static void cerrar() {
        if (jpa != null) {
            jpa.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void elAdministradorInicialPuedeIniciarSesion() {
        UsuarioSesion u = auth.login(System.getenv("ADMIN_EMAIL").toUpperCase(), System.getenv("ADMIN_PASSWORD"), "127.0.0.1");

        assertEquals(System.getenv("ADMIN_EMAIL").toLowerCase(), u.email().toLowerCase());
        assertEquals("ADMIN", u.rol());
    }

    @Test
    void unaContrasenaIncorrectaSeRechaza() {
        assertThrows(NoAutenticadoException.class,
                () -> auth.login(System.getenv("ADMIN_EMAIL"), System.getenv("ADMIN_PASSWORD") + "x", "127.0.0.2"));
    }
}

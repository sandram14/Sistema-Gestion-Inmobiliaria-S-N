package py.sn.inmobiliaria.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.exception.DemasiadosIntentosException;
import py.sn.inmobiliaria.exception.NoAutenticadoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.repository.UsuarioDao;
import py.sn.inmobiliaria.seguridad.ContrasenaHasher;
import py.sn.inmobiliaria.seguridad.LimitadorIntentos;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthServiceTest {

    /** Ejecuta el trabajo sin base de datos real (los DAOs de prueba ignoran el EntityManager). */
    static final class TxSinBase extends TxRunner {
        TxSinBase() {
            super(null);
        }

        @Override
        public <T> T soloLectura(Function<EntityManager, T> trabajo) {
            return trabajo.apply(null);
        }
    }

    private static final ContrasenaHasher HASHER = new ContrasenaHasher(4);

    static Usuario admin() {
        Rol rol = Entidades.crear(Rol.class, Map.of("id", 1, "nombre", "ADMIN"));
        Usuario u = new Usuario("Sandra", "Admin", "admin@sn.local", HASHER.hashear("Clave-Correcta"), rol);
        Entidades.asignar(u, "id", 7);
        return u;
    }

    /** DAO de prueba: un único usuario, búsqueda sin distinguir mayúsculas. */
    static final UsuarioDao DAO = (em, email) ->
            "admin@sn.local".equalsIgnoreCase(email) ? Optional.of(admin()) : Optional.empty();

    /** Service con un administrador admin@sn.local / Clave-Correcta (usado también por AutenticacionTest). */
    public static AuthService nuevoServicio() {
        return new AuthService(new TxSinBase(), DAO, HASHER, new LimitadorIntentos());
    }

    private final AuthService auth = nuevoServicio();

    @Test
    void loginCorrectoDevuelveElUsuarioSinDatosSensibles() {
        UsuarioSesion u = auth.login("  ADMIN@sn.local ", "Clave-Correcta", "10.0.0.1");

        assertEquals(7, u.id());
        assertEquals("Sandra", u.nombre());
        assertEquals("ADMIN", u.rol());
        assertTrue(u.esAdmin());
    }

    @Test
    void contrasenaIncorrectaYEmailInexistenteDanElMismoMensaje() {
        NoAutenticadoException clave = assertThrows(NoAutenticadoException.class,
                () -> auth.login("admin@sn.local", "mal", "10.0.0.1"));
        NoAutenticadoException email = assertThrows(NoAutenticadoException.class,
                () -> auth.login("nadie@sn.local", "mal", "10.0.0.1"));

        assertEquals(401, clave.estadoHttp());
        assertEquals(clave.getMessage(), email.getMessage());
        assertEquals("Email o contraseña incorrectos", clave.getMessage());
    }

    @Test
    void camposVaciosDan400() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> auth.login(" ", "", "10.0.0.1"));

        assertTrue(e.campos().containsKey("email"));
        assertTrue(e.campos().containsKey("password"));
    }

    @Test
    void seBloqueaTrasCincoFallosAunqueLuegoLaClaveSeaCorrecta() {
        for (int i = 0; i < 5; i++) {
            assertThrows(NoAutenticadoException.class, () -> auth.login("admin@sn.local", "mal", "10.0.0.1"));
        }

        DemasiadosIntentosException e = assertThrows(DemasiadosIntentosException.class,
                () -> auth.login("admin@sn.local", "Clave-Correcta", "10.0.0.1"));
        assertEquals(429, e.estadoHttp());
        // Desde otra IP sí puede ingresar
        assertEquals(7, auth.login("admin@sn.local", "Clave-Correcta", "10.0.0.2").id());
    }
}

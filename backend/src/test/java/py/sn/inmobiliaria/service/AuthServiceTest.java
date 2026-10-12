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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        @Override
        public <T> T enTransaccion(Function<EntityManager, T> trabajo) {
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

    public static Usuario conRol(int id, String email, String rol) {
        Usuario u = new Usuario("Persona", "Prueba", email, HASHER.hashear("Clave-Correcta"),
                Entidades.crear(Rol.class, Map.of("id", id, "nombre", rol)));
        Entidades.asignar(u, "id", id);
        return u;
    }

    /** DAO de prueba con usuarios en memoria; se pueden desactivar o cambiar de rol durante la prueba. */
    public static final class DaoFalso implements UsuarioDao {
        public final Map<Integer, Usuario> usuarios = new java.util.HashMap<>();

        public DaoFalso() {
            usuarios.put(7, admin());
        }

        @Override
        public Optional<Usuario> buscarActivoPorEmail(jakarta.persistence.EntityManager em, String email) {
            return usuarios.values().stream().filter(u -> u.isActivo() && u.getEmail().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public Optional<Usuario> buscarActivoPorId(jakarta.persistence.EntityManager em, int id) {
            return Optional.ofNullable(usuarios.get(id)).filter(Usuario::isActivo);
        }

        public final Map<Integer, Long> asignadas = new java.util.HashMap<>();

        @Override
        public Optional<Usuario> buscarPorId(jakarta.persistence.EntityManager em, int id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public java.util.List<Usuario> listar(jakarta.persistence.EntityManager em) {
            return new java.util.ArrayList<>(usuarios.values());
        }

        @Override
        public boolean existeEmail(jakarta.persistence.EntityManager em, String email, Integer excluirId) {
            return usuarios.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email) && !u.getId().equals(excluirId));
        }

        @Override
        public boolean existeCedula(jakarta.persistence.EntityManager em, String cedula, Integer excluirId) {
            return usuarios.values().stream().anyMatch(u -> cedula.equals(u.getCedula()) && !u.getId().equals(excluirId));
        }

        @Override
        public long contarAdministradoresActivos(jakarta.persistence.EntityManager em) {
            return usuarios.values().stream().filter(u -> u.isActivo() && "ADMIN".equals(u.getRol().getNombre())).count();
        }

        @Override
        public Map<Integer, Long> propiedadesPorResponsable(jakarta.persistence.EntityManager em) {
            return asignadas;
        }

        @Override
        public void guardar(jakarta.persistence.EntityManager em, Usuario usuario) {
            int id = usuarios.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
            Entidades.asignar(usuario, "id", id);
            usuarios.put(id, usuario);
        }
    }

    /** Service con un administrador admin@sn.local / Clave-Correcta (usado también por AutenticacionTest). */
    public static AuthService nuevoServicio() {
        return nuevoServicio(new DaoFalso());
    }

    public static AuthService nuevoServicio(DaoFalso dao) {
        return new AuthService(new TxSinBase(), dao, HASHER, new LimitadorIntentos());
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

    // ---- Etapa 2A: roles del panel y relectura del usuario ----

    @Test
    void gerenteYVendedorPuedenIngresarConSusPermisos() {
        DaoFalso dao = new DaoFalso();
        dao.usuarios.put(8, conRol(8, "gerente@sn.local", "GERENTE"));
        dao.usuarios.put(9, conRol(9, "vendedor@sn.local", "VENDEDOR"));
        AuthService s = nuevoServicio(dao);

        var gerente = s.login("gerente@sn.local", "Clave-Correcta", "1.1.1.1");
        var vendedor = s.login("vendedor@sn.local", "Clave-Correcta", "1.1.1.2");

        assertTrue(gerente.permisos().contains("USUARIOS_VER"));
        assertFalse(gerente.permisos().contains("USUARIOS_GESTIONAR"));
        assertTrue(vendedor.permisos().contains("PROPIEDADES_EDITAR"));
        assertFalse(vendedor.permisos().contains("PROPIEDADES_BAJA"));
    }

    @Test
    void unRolQueNoEsDelPanelNoPuedeIngresar() {
        DaoFalso dao = new DaoFalso();
        dao.usuarios.put(10, conRol(10, "otro@sn.local", "INVITADO"));

        assertThrows(py.sn.inmobiliaria.exception.NoAutenticadoException.class,
                () -> nuevoServicio(dao).login("otro@sn.local", "Clave-Correcta", "1.1.1.3"));
    }

    @Test
    void recargarReflejaLosCambiosDeLaBase() {
        DaoFalso dao = new DaoFalso();
        AuthService s = nuevoServicio(dao);
        assertEquals("ADMIN", s.recargar(7).orElseThrow().rol());

        dao.usuarios.get(7).setActivo(false);
        assertTrue(s.recargar(7).isEmpty());          // desactivado: pierde el acceso
        assertTrue(s.recargar(999).isEmpty());        // inexistente
    }
}

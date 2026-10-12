package py.sn.inmobiliaria.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.request.CuentaRequest;
import py.sn.inmobiliaria.dto.request.UsuarioRequest;
import py.sn.inmobiliaria.exception.ConflictoException;
import py.sn.inmobiliaria.exception.ReglaNegocioException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.prueba.Entidades;
import py.sn.inmobiliaria.repository.CatalogoDao;
import py.sn.inmobiliaria.seguridad.ContrasenaHasher;
import py.sn.inmobiliaria.seguridad.PoliticaContrasena;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** UsuarioService con usuarios en memoria (sin base): alta, edición, estado y cuenta propia. */
class UsuarioServiceTest {

    private static final ContrasenaHasher HASHER = new ContrasenaHasher();
    private AuthServiceTest.DaoFalso dao;
    private UsuarioService servicio;

    /** CatalogoDao de prueba: solo resuelve roles por nombre. */
    private static CatalogoDao catalogo() {
        return (CatalogoDao) Proxy.newProxyInstance(CatalogoDao.class.getClassLoader(), new Class<?>[] {CatalogoDao.class},
                (p, m, args) -> {
                    if (m.getName().equals("rolPorNombre")) {
                        String n = (String) args[1];
                        int id = switch (n) { case "ADMIN" -> 1; case "GERENTE" -> 2; case "VENDEDOR" -> 3; default -> 0; };
                        return id == 0 ? Optional.empty() : Optional.of(Entidades.crear(Rol.class, Map.of("id", id, "nombre", n)));
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
    }

    @BeforeEach
    void iniciar() {
        dao = new AuthServiceTest.DaoFalso(); // usuario 7: admin@sn.local, ADMIN, contraseña Clave-Correcta
        servicio = new UsuarioService(new AuthServiceTest.TxSinBase(), dao, catalogo(), HASHER);
    }

    private static UsuarioRequest req(String email, String rol) {
        return new UsuarioRequest("Ana", "Pérez", email, null, null, rol);
    }

    @Test
    void elAltaGeneraUnaContrasenaTemporalQueSeDebeCambiar() {
        var creado = servicio.crear(req("ana@sn.local", "VENDEDOR"));

        assertNull(PoliticaContrasena.problema(creado.contrasenaTemporal()));
        assertEquals("VENDEDOR", creado.usuario().rol());
        assertTrue(creado.usuario().debeCambiarPassword());
        var guardado = dao.usuarios.get(creado.usuario().id());
        assertTrue(HASHER.verificar(creado.contrasenaTemporal(), guardado.getPasswordHash())); // se guarda solo el hash
        assertNotEquals(creado.contrasenaTemporal(), guardado.getPasswordHash());
    }

    @Test
    void validaDatosYRol() {
        var e = assertThrows(ValidacionException.class, () -> servicio.crear(new UsuarioRequest("", "", "no-es-email", null, null, "JEFE")));
        assertTrue(e.campos().keySet().containsAll(java.util.Set.of("nombre", "apellido", "email", "rol")));
    }

    @Test
    void elEmailNoSePuedeRepetirSinImportarMayusculas() {
        assertThrows(ConflictoException.class, () -> servicio.crear(req("ADMIN@sn.local", "GERENTE")));
    }

    @Test
    void nadieCambiaSuPropioRolNiSeDesactivaASiMismo() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(7, new UsuarioRequest("S", "A", "admin@sn.local", null, null, "VENDEDOR"), 7));
        assertThrows(ReglaNegocioException.class, () -> servicio.cambiarEstado(7, false, 7));
        assertThrows(ReglaNegocioException.class, () -> servicio.restablecerContrasena(7, 7));
    }

    @Test
    void siempreQuedaAlMenosUnAdministradorActivo() {
        int otroAdmin = servicio.crear(req("otro@sn.local", "ADMIN")).usuario().id();
        servicio.cambiarEstado(7, false, otroAdmin);           // había dos: se puede
        var e = assertThrows(ReglaNegocioException.class, () -> servicio.cambiarEstado(otroAdmin, false, 99));
        assertEquals("REGLA_ULTIMO_ADMIN", e.codigo());
        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizar(otroAdmin, new UsuarioRequest("A", "B", "otro@sn.local", null, null, "GERENTE"), 99));
    }

    @Test
    void elAdministradorEditaDatosYRolDeOtroYRestableceSuContrasena() {
        int id = servicio.crear(req("ana@sn.local", "VENDEDOR")).usuario().id();
        dao.usuarios.get(id).setDebeCambiarPassword(false);

        var editado = servicio.actualizar(id, new UsuarioRequest("Ana", "Gómez", "ana.gomez@sn.local", "1234567", "0981", "GERENTE"), 7);
        assertEquals("GERENTE", editado.rol());
        assertEquals("ana.gomez@sn.local", editado.email());

        var nueva = servicio.restablecerContrasena(id, 7);
        assertTrue(dao.usuarios.get(id).isDebeCambiarPassword());
        assertTrue(HASHER.verificar(nueva.contrasenaTemporal(), dao.usuarios.get(id).getPasswordHash()));
    }

    @Test
    void cambiarMiContrasenaExigeLaActualYCumplirLasReglas() {
        var mal = assertThrows(ValidacionException.class,
                () -> servicio.cambiarMiContrasena(7, new CuentaRequest.CambioContrasena("incorrecta", "NuevaClave123")));
        assertTrue(mal.campos().containsKey("passwordActual"));
        var corta = assertThrows(ValidacionException.class,
                () -> servicio.cambiarMiContrasena(7, new CuentaRequest.CambioContrasena("Clave-Correcta", "corta1")));
        assertTrue(corta.campos().containsKey("passwordNueva"));
        assertThrows(ValidacionException.class,
                () -> servicio.cambiarMiContrasena(7, new CuentaRequest.CambioContrasena("Clave-Correcta", "SoloLetrasLargas")));

        dao.usuarios.get(7).setDebeCambiarPassword(true);
        servicio.cambiarMiContrasena(7, new CuentaRequest.CambioContrasena("Clave-Correcta", "NuevaClave123"));
        assertTrue(HASHER.verificar("NuevaClave123", dao.usuarios.get(7).getPasswordHash()));
        assertFalse(dao.usuarios.get(7).isDebeCambiarPassword()); // la contraseña temporal quedó cambiada
    }

    @Test
    void cambiarMiEmailExigeLaContrasenaYUnEmailLibre() {
        servicio.crear(req("ocupado@sn.local", "VENDEDOR"));
        assertThrows(ValidacionException.class, () -> servicio.cambiarMiEmail(7, new CuentaRequest.CambioEmail("nuevo@sn.local", "mal")));
        assertThrows(ConflictoException.class, () -> servicio.cambiarMiEmail(7, new CuentaRequest.CambioEmail("OCUPADO@sn.local", "Clave-Correcta")));
        assertEquals("nuevo@sn.local", servicio.cambiarMiEmail(7, new CuentaRequest.CambioEmail("nuevo@sn.local", "Clave-Correcta")).email());
    }

    @Test
    void misDatosNoIncluyenRolNiEmail() {
        var r = servicio.actualizarMisDatos(7, new CuentaRequest.Datos("Sandra", "Gómez", "4444444", "0985"));
        assertEquals("Gómez", r.apellido());
        assertEquals("ADMIN", r.rol());
        assertEquals("admin@sn.local", r.email());
    }

    @Test
    void elListadoIncluyeLasPropiedadesAsignadas() {
        dao.asignadas.put(7, 4L);
        assertEquals(4L, servicio.listar().stream().filter(u -> u.id() == 7).findFirst().orElseThrow().propiedadesAsignadas());
    }
}

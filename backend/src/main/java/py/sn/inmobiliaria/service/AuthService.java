package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.exception.DemasiadosIntentosException;
import py.sn.inmobiliaria.exception.NoAutenticadoException;
import py.sn.inmobiliaria.repository.UsuarioDao;
import py.sn.inmobiliaria.seguridad.ContrasenaHasher;
import py.sn.inmobiliaria.seguridad.LimitadorIntentos;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.validacion.Errores;

import java.util.Optional;

/**
 * Login de administradores.
 *
 * <p>Ante un error siempre responde el mismo mensaje ("Email o contraseña incorrectos")
 * y tarda lo mismo, exista o no el email: así no se puede averiguar qué emails están
 * registrados.</p>
 */
public class AuthService extends ServicioBase {

    static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos";

    private final UsuarioDao usuarioDao;
    private final ContrasenaHasher hasher;
    private final LimitadorIntentos limitador;

    public AuthService(TxRunner tx, UsuarioDao usuarioDao, ContrasenaHasher hasher, LimitadorIntentos limitador) {
        super(tx);
        this.usuarioDao = usuarioDao;
        this.hasher = hasher;
        this.limitador = limitador;
    }

    /** Datos leídos de la base: el usuario para la sesión y el hash para comparar. */
    private record Candidato(UsuarioSesion usuario, String hash) {
    }

    /**
     * @param ip dirección del cliente (para el límite de intentos)
     * @return el usuario autenticado, para guardar en la sesión
     * @throws NoAutenticadoException       si las credenciales no son válidas (401)
     * @throws DemasiadosIntentosException  si hubo demasiados fallos recientes (429)
     */
    public UsuarioSesion login(String email, String contrasena, String ip) {
        Errores errores = new Errores();
        if (email == null || email.isBlank()) {
            errores.agregar("email", "Ingrese su email");
        }
        if (contrasena == null || contrasena.isEmpty()) {
            errores.agregar("password", "Ingrese su contraseña");
        }
        errores.lanzarSiHay();
        String emailLimpio = email.trim();

        long minutos = limitador.minutosDeBloqueo(ip, emailLimpio);
        if (minutos > 0) {
            throw new DemasiadosIntentosException(minutos);
        }

        // Solo la lectura ocurre dentro de la transacción; la comparación BCrypt (lenta, a
        // propósito) se hace después, para no retener una conexión de la base.
        Optional<Candidato> candidato = leer(em -> usuarioDao.buscarActivoPorEmail(em, emailLimpio)
                .map(u -> new Candidato(new UsuarioSesion(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(),
                        u.getRol().getNombre()), u.getPasswordHash())));

        boolean valido;
        if (candidato.isPresent()) {
            valido = hasher.verificar(contrasena, candidato.get().hash()) && candidato.get().usuario().esAdmin();
        } else {
            hasher.verificarFicticio(contrasena); // mismo tiempo de respuesta que con un email existente
            valido = false;
        }

        if (!valido) {
            limitador.registrarFallo(ip, emailLimpio);
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        limitador.registrarExito(ip, emailLimpio);
        return candidato.get().usuario();
    }
}

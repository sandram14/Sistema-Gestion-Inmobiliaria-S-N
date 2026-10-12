package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.request.CuentaRequest;
import py.sn.inmobiliaria.dto.request.UsuarioRequest;
import py.sn.inmobiliaria.dto.response.admin.UsuarioDto;
import py.sn.inmobiliaria.exception.ConflictoException;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ReglaNegocioException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Rol;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.repository.CatalogoDao;
import py.sn.inmobiliaria.repository.UsuarioDao;
import py.sn.inmobiliaria.seguridad.ContrasenaHasher;
import py.sn.inmobiliaria.seguridad.Permiso;
import py.sn.inmobiliaria.seguridad.PoliticaContrasena;
import py.sn.inmobiliaria.service.validacion.Errores;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Usuarios del panel y cuenta propia.
 *
 * <p>Protecciones (además de los permisos que exige AuthFilter):</p>
 * <ul>
 *   <li>nadie cambia su propio rol ni se desactiva a sí mismo;</li>
 *   <li>siempre queda al menos un administrador activo;</li>
 *   <li>el email (sin distinguir mayúsculas) y la cédula no se repiten;</li>
 *   <li>cambiar el email o la contraseña propios exige la contraseña actual;</li>
 *   <li>los usuarios no se borran: se desactivan, para conservar el historial.</li>
 * </ul>
 */
public class UsuarioService extends ServicioBase {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioDao usuarioDao;
    private final CatalogoDao catalogoDao;
    private final ContrasenaHasher hasher;

    public UsuarioService(TxRunner tx, UsuarioDao usuarioDao, CatalogoDao catalogoDao, ContrasenaHasher hasher) {
        super(tx);
        this.usuarioDao = usuarioDao;
        this.catalogoDao = catalogoDao;
        this.hasher = hasher;
    }

    // ---- Gestión de usuarios (ADMIN; el GERENTE solo consulta) ----

    public List<UsuarioDto> listar() {
        return leer(em -> {
            Map<Integer, Long> asignadas = usuarioDao.propiedadesPorResponsable(em);
            return usuarioDao.listar(em).stream().map(u -> dto(u, asignadas.getOrDefault(u.getId(), 0L))).toList();
        });
    }

    public UsuarioDto obtener(int id) {
        return leer(em -> {
            Usuario u = buscar(em, id);
            return dto(u, usuarioDao.propiedadesPorResponsable(em).getOrDefault(id, 0L));
        });
    }

    /** Crea el usuario con una contraseña temporal generada por el sistema, que deberá cambiar al ingresar. */
    public UsuarioDto.ConContrasenaTemporal crear(UsuarioRequest req) {
        Errores e = new Errores();
        Datos d = validar(req, e, true);
        e.lanzarSiHay();
        String temporal = PoliticaContrasena.generarTemporal();
        String hash = hasher.hashear(temporal); // BCrypt fuera de la transacción
        UsuarioDto creado = escribir(em -> {
            verificarUnicos(em, d, null);
            Usuario u = new Usuario(d.nombre(), d.apellido(), d.email(), hash, rol(em, d.rol()));
            u.setCedula(d.cedula());
            u.setTelefono(d.telefono());
            u.setDebeCambiarPassword(true);
            usuarioDao.guardar(em, u);
            return dto(u, 0);
        });
        return new UsuarioDto.ConContrasenaTemporal(creado, temporal);
    }

    public UsuarioDto actualizar(int id, UsuarioRequest req, int idActor) {
        Errores e = new Errores();
        Datos d = validar(req, e, true);
        e.lanzarSiHay();
        return escribir(em -> {
            Usuario u = buscar(em, id);
            verificarUnicos(em, d, id);
            String rolActual = u.getRol().getNombre();
            if (!rolActual.equals(d.rol())) {
                if (id == idActor) {
                    throw new ReglaNegocioException("REGLA_PROPIO_ROL", "No puede cambiar su propio rol");
                }
                if (Permiso.ADMIN.equals(rolActual) && u.isActivo() && usuarioDao.contarAdministradoresActivos(em) <= 1) {
                    throw new ReglaNegocioException("REGLA_ULTIMO_ADMIN", "Debe quedar al menos un administrador activo");
                }
                u.setRol(rol(em, d.rol()));
            }
            u.setNombre(d.nombre());
            u.setApellido(d.apellido());
            u.setEmail(d.email());
            u.setCedula(d.cedula());
            u.setTelefono(d.telefono());
            return dto(u, usuarioDao.propiedadesPorResponsable(em).getOrDefault(id, 0L));
        });
    }

    public UsuarioDto cambiarEstado(int id, Boolean activo, int idActor) {
        if (activo == null) {
            throw ValidacionException.deCampo("activo", "Indique si la cuenta queda activa");
        }
        return escribir(em -> {
            Usuario u = buscar(em, id);
            if (!activo && u.isActivo()) {
                if (id == idActor) {
                    throw new ReglaNegocioException("REGLA_PROPIA_CUENTA", "No puede desactivar su propia cuenta");
                }
                if (Permiso.ADMIN.equals(u.getRol().getNombre()) && usuarioDao.contarAdministradoresActivos(em) <= 1) {
                    throw new ReglaNegocioException("REGLA_ULTIMO_ADMIN", "Debe quedar al menos un administrador activo");
                }
            }
            u.setActivo(activo);
            return dto(u, usuarioDao.propiedadesPorResponsable(em).getOrDefault(id, 0L));
        });
    }

    /** Genera una nueva contraseña temporal (por ejemplo, si el usuario olvidó la suya). */
    public UsuarioDto.ConContrasenaTemporal restablecerContrasena(int id, int idActor) {
        if (id == idActor) {
            throw new ReglaNegocioException("REGLA_PROPIA_CUENTA", "Para su propia cuenta use \"Cambiar contraseña\" en Mi cuenta");
        }
        String temporal = PoliticaContrasena.generarTemporal();
        String hash = hasher.hashear(temporal);
        UsuarioDto dto = escribir(em -> {
            Usuario u = buscar(em, id);
            u.setPasswordHash(hash);
            u.setDebeCambiarPassword(true);
            return dto(u, usuarioDao.propiedadesPorResponsable(em).getOrDefault(id, 0L));
        });
        return new UsuarioDto.ConContrasenaTemporal(dto, temporal);
    }

    // ---- Cuenta propia (todos los roles) ----

    public UsuarioDto actualizarMisDatos(int idActor, CuentaRequest.Datos req) {
        Errores e = new Errores();
        String nombre = obligatorio(req == null ? null : req.nombre(), "nombre", 60, e);
        String apellido = obligatorio(req == null ? null : req.apellido(), "apellido", 60, e);
        String cedula = opcional(req == null ? null : req.cedula(), "cedula", 20, e);
        String telefono = opcional(req == null ? null : req.telefono(), "telefono", 30, e);
        e.lanzarSiHay();
        return escribir(em -> {
            Usuario u = buscar(em, idActor);
            if (cedula != null && usuarioDao.existeCedula(em, cedula, idActor)) {
                throw new ConflictoException("Ya existe otro usuario con esa cédula");
            }
            u.setNombre(nombre);
            u.setApellido(apellido);
            u.setCedula(cedula);
            u.setTelefono(telefono);
            return dto(u, usuarioDao.propiedadesPorResponsable(em).getOrDefault(idActor, 0L));
        });
    }

    public UsuarioDto cambiarMiEmail(int idActor, CuentaRequest.CambioEmail req) {
        Errores e = new Errores();
        String email = email(req == null ? null : req.email(), e);
        if (req == null || req.passwordActual() == null || req.passwordActual().isEmpty()) {
            e.agregar("passwordActual", "Ingrese su contraseña actual");
        }
        e.lanzarSiHay();
        verificarContrasenaActual(idActor, req.passwordActual());
        return escribir(em -> {
            Usuario u = buscar(em, idActor);
            if (usuarioDao.existeEmail(em, email, idActor)) {
                throw new ConflictoException("Ya existe otro usuario con ese email");
            }
            u.setEmail(email);
            return dto(u, usuarioDao.propiedadesPorResponsable(em).getOrDefault(idActor, 0L));
        });
    }

    /** Cambia la contraseña propia y quita la marca de contraseña temporal. */
    public void cambiarMiContrasena(int idActor, CuentaRequest.CambioContrasena req) {
        Errores e = new Errores();
        if (req == null || req.passwordActual() == null || req.passwordActual().isEmpty()) {
            e.agregar("passwordActual", "Ingrese su contraseña actual");
        }
        String problema = PoliticaContrasena.problema(req == null ? null : req.passwordNueva());
        if (problema != null) {
            e.agregar("passwordNueva", problema);
        } else if (req.passwordNueva().equals(req.passwordActual())) {
            e.agregar("passwordNueva", "La nueva contraseña debe ser distinta de la actual");
        }
        e.lanzarSiHay();
        verificarContrasenaActual(idActor, req.passwordActual());
        String hash = hasher.hashear(req.passwordNueva());
        escribir(em -> {
            Usuario u = buscar(em, idActor);
            u.setPasswordHash(hash);
            u.setDebeCambiarPassword(false);
            return null;
        });
    }

    // ---- Apoyo ----

    private record Datos(String nombre, String apellido, String email, String cedula, String telefono, String rol) {
    }

    private Datos validar(UsuarioRequest req, Errores e, boolean conRol) {
        String nombre = obligatorio(req == null ? null : req.nombre(), "nombre", 60, e);
        String apellido = obligatorio(req == null ? null : req.apellido(), "apellido", 60, e);
        String email = email(req == null ? null : req.email(), e);
        String cedula = opcional(req == null ? null : req.cedula(), "cedula", 20, e);
        String telefono = opcional(req == null ? null : req.telefono(), "telefono", 30, e);
        String rol = req == null || req.rol() == null ? null : req.rol().trim().toUpperCase(java.util.Locale.ROOT);
        if (conRol && !Permiso.esRolValido(rol)) {
            e.agregar("rol", "Elija un rol: ADMIN, GERENTE o VENDEDOR");
        }
        return new Datos(nombre, apellido, email, cedula, telefono, rol);
    }

    private void verificarUnicos(EntityManager em, Datos d, Integer excluir) {
        if (usuarioDao.existeEmail(em, d.email(), excluir)) {
            throw new ConflictoException("Ya existe otro usuario con ese email");
        }
        if (d.cedula() != null && usuarioDao.existeCedula(em, d.cedula(), excluir)) {
            throw new ConflictoException("Ya existe otro usuario con esa cédula");
        }
    }

    private void verificarContrasenaActual(int idActor, String actual) {
        String hash = leer(em -> buscar(em, idActor).getPasswordHash());
        if (!hasher.verificar(actual, hash)) {
            throw ValidacionException.deCampo("passwordActual", "La contraseña actual no es correcta");
        }
    }

    private Usuario buscar(EntityManager em, int id) {
        return usuarioDao.buscarPorId(em, id).orElseThrow(() -> new NoEncontradoException("El usuario no existe"));
    }

    private Rol rol(EntityManager em, String nombre) {
        return catalogoDao.rolPorNombre(em, nombre)
                .orElseThrow(() -> ValidacionException.deCampo("rol", "El rol no existe en la base de datos"));
    }

    private static String obligatorio(String valor, String campo, int max, Errores e) {
        String v = valor == null ? "" : valor.strip();
        if (v.isEmpty()) {
            e.agregar(campo, "Es obligatorio");
        } else if (v.length() > max) {
            e.agregar(campo, "Máximo " + max + " caracteres");
        }
        return v;
    }

    private static String opcional(String valor, String campo, int max, Errores e) {
        String v = valor == null ? "" : valor.strip();
        if (v.length() > max) {
            e.agregar(campo, "Máximo " + max + " caracteres");
        }
        return v.isEmpty() ? null : v;
    }

    private static String email(String valor, Errores e) {
        String v = valor == null ? "" : valor.strip();
        if (v.isEmpty()) {
            e.agregar("email", "Es obligatorio");
        } else if (v.length() > 120 || !EMAIL.matcher(v).matches()) {
            e.agregar("email", "Ingrese un email válido");
        }
        return v;
    }

    static UsuarioDto dto(Usuario u, long asignadas) {
        return new UsuarioDto(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(), u.getCedula(), u.getTelefono(),
                u.getRol().getNombre(), u.isActivo(), u.isDebeCambiarPassword(), u.getCreadoEn(), asignadas);
    }
}

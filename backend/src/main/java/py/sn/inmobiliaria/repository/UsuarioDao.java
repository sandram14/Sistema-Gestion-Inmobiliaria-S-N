package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Usuario;

import java.util.Optional;

/** Acceso a usuarios administrativos (usado por el login en la Fase 10). */
public interface UsuarioDao {

    /** Usuario activo por email, sin distinguir mayúsculas. */
    Optional<Usuario> buscarActivoPorEmail(EntityManager em, String email);

    /** Usuario activo por id, con su rol (para releerlo en cada petición). */
    Optional<Usuario> buscarActivoPorId(EntityManager em, int id);

    /** Usuario por id, activo o no, con su rol. */
    Optional<Usuario> buscarPorId(EntityManager em, int id);

    /** Todos los usuarios con su rol, por apellido y nombre. */
    java.util.List<Usuario> listar(EntityManager em);

    /** ¿Otro usuario (distinto de excluirId) usa ese email? Sin distinguir mayúsculas. */
    boolean existeEmail(EntityManager em, String email, Integer excluirId);

    /** ¿Otro usuario (distinto de excluirId) usa esa cédula? */
    boolean existeCedula(EntityManager em, String cedula, Integer excluirId);

    /** Administradores activos. */
    long contarAdministradoresActivos(EntityManager em);

    /** Cantidad de propiedades asignadas a cada usuario (solo los que tienen alguna). */
    java.util.Map<Integer, Long> propiedadesPorResponsable(EntityManager em);

    void guardar(EntityManager em, Usuario usuario);

    /**
     * Registra una operación en la tabla auditoria (migración 011), en la misma transacción que la operación.
     * Nunca recibe contraseñas: solo la acción, quién la hizo y a quién afectó.
     */
    void registrarAuditoria(EntityManager em, String accion, int idActor, int idUsuarioAfectado);
}

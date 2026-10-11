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
}

package py.sn.inmobiliaria.repository;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.model.Usuario;

import java.util.Optional;

/** Acceso a usuarios administrativos (usado por el login en la Fase 10). */
public interface UsuarioDao {

    /** Usuario activo por email, sin distinguir mayúsculas. */
    Optional<Usuario> buscarActivoPorEmail(EntityManager em, String email);
}

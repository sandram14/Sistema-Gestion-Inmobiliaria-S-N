package py.sn.inmobiliaria.seguridad;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permisos del panel y su asignación FIJA por rol (no se editan permisos sueltos).
 *
 * <p>El backend los comprueba en cada petición (ver {@link ReglasAcceso} y AuthFilter):
 * ocultar una opción en el panel no alcanza para impedirla.</p>
 */
public enum Permiso {
    PROPIEDADES_VER,
    /** Crear propiedades y editar las propias (las que tiene asignadas). */
    PROPIEDADES_EDITAR,
    /** Editar cualquier propiedad y asignar su responsable. */
    PROPIEDADES_EDITAR_TODAS,
    /** Dar de baja y reactivar. */
    PROPIEDADES_BAJA,
    OFERTAS_GESTIONAR,
    CONTENIDO_EDITAR,
    CONFIGURACION_VER,
    CONFIGURACION_EDITAR,
    /** Consultas del chat IA: las generales y las de sus propiedades. */
    CONSULTAS_VER,
    /** Consultas del chat IA: todas. */
    CONSULTAS_VER_TODAS,
    ESTADISTICAS_VER,
    USUARIOS_VER,
    /** Crear, editar, asignar rol, activar y desactivar usuarios. */
    USUARIOS_GESTIONAR,
    /** Ver y editar los datos de la propia cuenta. Lo tiene todo usuario del panel. */
    CUENTA_PROPIA;

    public static final String ADMIN = "ADMIN";
    public static final String GERENTE = "GERENTE";
    public static final String VENDEDOR = "VENDEDOR";

    private static final Map<String, Set<Permiso>> POR_ROL = Map.of(
            ADMIN, EnumSet.allOf(Permiso.class),
            GERENTE, EnumSet.of(PROPIEDADES_VER, PROPIEDADES_EDITAR, PROPIEDADES_EDITAR_TODAS, PROPIEDADES_BAJA,
                    OFERTAS_GESTIONAR, CONTENIDO_EDITAR, CONFIGURACION_VER, CONSULTAS_VER, CONSULTAS_VER_TODAS,
                    ESTADISTICAS_VER, USUARIOS_VER, CUENTA_PROPIA),
            VENDEDOR, EnumSet.of(PROPIEDADES_VER, PROPIEDADES_EDITAR, CONSULTAS_VER, CUENTA_PROPIA));

    /** Roles que pueden usar el panel, en orden de mayor a menor alcance. */
    public static final List<String> ROLES = List.of(ADMIN, GERENTE, VENDEDOR);

    /** Permisos del rol; un rol desconocido no tiene ninguno. */
    public static Set<Permiso> deRol(String rol) {
        Set<Permiso> permisos = rol == null ? null : POR_ROL.get(rol);
        return permisos == null ? EnumSet.noneOf(Permiso.class) : EnumSet.copyOf(permisos);
    }

    public static boolean esRolValido(String rol) {
        return rol != null && POR_ROL.containsKey(rol);
    }
}

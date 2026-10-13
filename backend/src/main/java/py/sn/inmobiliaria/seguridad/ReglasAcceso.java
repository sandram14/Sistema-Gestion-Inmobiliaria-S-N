package py.sn.inmobiliaria.seguridad;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Qué permiso exige cada endpoint administrativo. Es el único lugar donde se define.
 *
 * <p>Regla de seguridad: un endpoint de /api/admin/* que no figure aquí queda BLOQUEADO
 * (403). Así, olvidarse de declarar una regla nunca abre un acceso por error.</p>
 *
 * <p>Las comprobaciones que dependen del dato (por ejemplo, que un vendedor solo edite
 * las propiedades asignadas a él) se hacen además en los services.</p>
 */
public final class ReglasAcceso {

    private ReglasAcceso() {
    }

    private record Regla(String metodo, Pattern ruta, Permiso permiso) {
    }

    private static Regla r(String metodo, String ruta, Permiso permiso) {
        return new Regla(metodo, Pattern.compile("^" + ruta + "$"), permiso);
    }

    private static final String ID = "[0-9]+";

    private static final List<Regla> REGLAS = List.of(
            // Sesión
            r("GET", "/api/admin/auth/me", Permiso.CUENTA_PROPIA),
            // Propiedades
            r("GET", "/api/admin/propiedades", Permiso.PROPIEDADES_VER),
            r("GET", "/api/admin/propiedades/" + ID, Permiso.PROPIEDADES_VER),
            r("POST", "/api/admin/propiedades", Permiso.PROPIEDADES_EDITAR),
            r("PUT", "/api/admin/propiedades/" + ID, Permiso.PROPIEDADES_EDITAR),
            r("DELETE", "/api/admin/propiedades/" + ID, Permiso.PROPIEDADES_BAJA),
            r("POST", "/api/admin/propiedades/" + ID + "/imagenes", Permiso.PROPIEDADES_EDITAR),
            r("DELETE", "/api/admin/propiedades/" + ID + "/imagenes/" + ID, Permiso.PROPIEDADES_EDITAR),
            r("PATCH", "/api/admin/propiedades/" + ID + "/imagenes/" + ID + "/principal", Permiso.PROPIEDADES_EDITAR),
            r("GET", "/api/admin/ubicacion", Permiso.PROPIEDADES_EDITAR),
            // Usuarios: consultar (ADMIN y GERENTE) y gestionar (solo ADMIN)
            r("GET", "/api/admin/usuarios", Permiso.USUARIOS_VER),
            r("GET", "/api/admin/usuarios/" + ID, Permiso.USUARIOS_VER),
            r("POST", "/api/admin/usuarios", Permiso.USUARIOS_GESTIONAR),
            r("PUT", "/api/admin/usuarios/" + ID, Permiso.USUARIOS_GESTIONAR),
            r("PATCH", "/api/admin/usuarios/" + ID + "/estado", Permiso.USUARIOS_GESTIONAR),
            r("POST", "/api/admin/usuarios/" + ID + "/contrasena-temporal", Permiso.USUARIOS_GESTIONAR),
            // Consultas del chat IA: historial (el vendedor ve solo las suyas) y análisis de todo el sitio
            r("GET", "/api/admin/consultas", Permiso.CONSULTAS_VER),
            r("GET", "/api/admin/consultas/analisis", Permiso.CONSULTAS_VER_TODAS),
            r("GET", "/api/admin/consultas/" + ID, Permiso.CONSULTAS_VER),
            r("PATCH", "/api/admin/consultas/" + ID + "/revision", Permiso.CONSULTAS_VER),
            // Cuenta propia
            r("GET", "/api/admin/cuenta", Permiso.CUENTA_PROPIA),
            r("PUT", "/api/admin/cuenta", Permiso.CUENTA_PROPIA),
            r("PUT", "/api/admin/cuenta/email", Permiso.CUENTA_PROPIA),
            r("PUT", "/api/admin/cuenta/contrasena", Permiso.CUENTA_PROPIA));

    /** Rutas permitidas mientras el usuario tiene una contraseña temporal sin cambiar. */
    private static final List<Regla> CON_CONTRASENA_TEMPORAL = List.of(
            r("GET", "/api/admin/auth/me", Permiso.CUENTA_PROPIA),
            r("GET", "/api/admin/cuenta", Permiso.CUENTA_PROPIA),
            r("PUT", "/api/admin/cuenta/contrasena", Permiso.CUENTA_PROPIA));

    /** ¿La ruta se puede usar antes de cambiar la contraseña temporal? */
    public static boolean permitidaConContrasenaTemporal(String metodo, String ruta) {
        String m = metodo.toUpperCase(Locale.ROOT);
        return CON_CONTRASENA_TEMPORAL.stream().anyMatch(r -> r.metodo().equals(m) && r.ruta().matcher(ruta).matches());
    }

    /**
     * @param metodo método HTTP
     * @param ruta   ruta sin el contexto (por ejemplo, /api/admin/propiedades/12)
     * @return el permiso exigido, o vacío si la ruta no tiene regla (debe bloquearse)
     */
    public static Optional<Permiso> permisoPara(String metodo, String ruta) {
        String m = metodo.toUpperCase(Locale.ROOT);
        String sinBarraFinal = ruta.length() > 1 && ruta.endsWith("/") ? ruta.substring(0, ruta.length() - 1) : ruta;
        return REGLAS.stream()
                .filter(regla -> regla.metodo().equals(m) && regla.ruta().matcher(sinBarraFinal).matches())
                .map(Regla::permiso)
                .findFirst();
    }
}

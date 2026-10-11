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
            r("GET", "/api/admin/ubicacion", Permiso.PROPIEDADES_EDITAR));

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

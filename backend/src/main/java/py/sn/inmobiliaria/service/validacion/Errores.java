package py.sn.inmobiliaria.service.validacion;

import py.sn.inmobiliaria.exception.ValidacionException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Acumula errores de validación campo por campo, para informarlos todos juntos
 * (en lugar de que el usuario los descubra de a uno).
 */
public final class Errores {

    private final Map<String, String> campos = new LinkedHashMap<>();

    /** Registra el error; si el campo ya tenía uno, conserva el primero. */
    public void agregar(String campo, String mensaje) {
        campos.putIfAbsent(campo, mensaje);
    }

    public boolean hay() {
        return !campos.isEmpty();
    }

    /** Lanza ValidacionException si se registró algún error. */
    public void lanzarSiHay() {
        if (hay()) {
            throw new ValidacionException(campos);
        }
    }

    public Map<String, String> comoMapa() {
        return Map.copyOf(campos);
    }
}

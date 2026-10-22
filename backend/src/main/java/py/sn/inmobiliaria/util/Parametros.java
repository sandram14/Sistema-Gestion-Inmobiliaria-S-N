package py.sn.inmobiliaria.util;

import jakarta.servlet.http.HttpServletRequest;
import py.sn.inmobiliaria.service.validacion.Errores;

import java.math.BigDecimal;

/**
 * Lectura segura de parámetros de la URL (?ciudadId=1&presupuestoMax=50000).
 *
 * <p>Un parámetro vacío se trata como ausente. Si un valor no tiene el formato esperado,
 * se registra el error del campo en lugar de lanzar una excepción técnica, para responder
 * 400 con todos los errores juntos.</p>
 */
public final class Parametros {

    private Parametros() {
    }

    /** Primer parámetro no vacío entre los nombres dados (permite alias, como tipoOperacion = operacion). */
    public static String texto(HttpServletRequest req, String... nombres) {
        for (String nombre : nombres) {
            String valor = req.getParameter(nombre);
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }

    public static Integer entero(HttpServletRequest req, String nombre, Errores errores) {
        String valor = texto(req, nombre);
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            errores.agregar(nombre, "Debe ser un número entero");
            return null;
        }
    }

    /** "true" o "false" (sin distinguir mayúsculas); ausente o vacío → false. */
    public static boolean booleano(HttpServletRequest req, String nombre, Errores errores) {
        String valor = texto(req, nombre);
        if (valor == null || valor.equalsIgnoreCase("false")) {
            return false;
        }
        if (valor.equalsIgnoreCase("true")) {
            return true;
        }
        errores.agregar(nombre, "Debe ser true o false");
        return false;
    }

    public static BigDecimal decimal(HttpServletRequest req, String nombre, Errores errores) {
        String valor = texto(req, nombre);
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            errores.agregar(nombre, "Debe ser un número (use punto como separador decimal)");
            return null;
        }
    }
}

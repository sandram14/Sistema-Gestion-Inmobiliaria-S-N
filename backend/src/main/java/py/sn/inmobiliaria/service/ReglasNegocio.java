package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.TipoPropiedad;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reglas del negocio inmobiliario, definidas en un solo lugar.
 *
 * <p>La base de datos las vuelve a verificar con triggers (última barrera); aquí se
 * validan primero para responder con mensajes claros.</p>
 */
public final class ReglasNegocio {

    /** Qué operaciones admite cada tipo de propiedad. */
    private static final Map<String, List<String>> OPERACIONES_POR_TIPO;

    static {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(TipoPropiedad.CASA, List.of(Operacion.VENTA, Operacion.ALQUILER));
        m.put(TipoPropiedad.TERRENO, List.of(Operacion.VENTA));
        OPERACIONES_POR_TIPO = Map.copyOf(m);
    }

    private ReglasNegocio() {
    }

    /** Operaciones permitidas por tipo: {Casa: [Venta, Alquiler], Terreno: [Venta]}. */
    public static Map<String, List<String>> operacionesPorTipo() {
        Map<String, List<String>> ordenado = new LinkedHashMap<>();
        ordenado.put(TipoPropiedad.CASA, OPERACIONES_POR_TIPO.get(TipoPropiedad.CASA));
        ordenado.put(TipoPropiedad.TERRENO, OPERACIONES_POR_TIPO.get(TipoPropiedad.TERRENO));
        return ordenado;
    }

    public static boolean operacionPermitida(String tipo, String operacion) {
        return OPERACIONES_POR_TIPO.getOrDefault(tipo, List.of()).contains(operacion);
    }

    /** Prefijo del código público según el tipo: prop-017 (casa), terr-013 (terreno). */
    public static String prefijoCodigo(String tipo) {
        return TipoPropiedad.TERRENO.equals(tipo) ? "terr" : "prop";
    }

    /** "prop" + 17 -> "prop-017" (al menos 3 dígitos, como los códigos existentes). */
    public static String formatearCodigo(String prefijo, int numero) {
        return String.format(Locale.ROOT, "%s-%03d", prefijo, numero);
    }

    // ---- Normalización de valores de catálogo (sin distinguir mayúsculas) ----

    /** "casa", "CASA" -> "Casa"; "vivienda" (término del frontend) -> "Casa". */
    public static Optional<String> normalizarTipo(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String v = valor.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "casa", "vivienda" -> Optional.of(TipoPropiedad.CASA);
            case "terreno" -> Optional.of(TipoPropiedad.TERRENO);
            default -> Optional.empty();
        };
    }

    public static Optional<String> normalizarOperacion(String valor) {
        return coincidir(valor, Operacion.VENTA, Operacion.ALQUILER);
    }

    public static Optional<String> normalizarEstado(String valor) {
        return coincidir(valor, EstadoPropiedad.DISPONIBLE, EstadoPropiedad.RESERVADO,
                EstadoPropiedad.VENDIDO, EstadoPropiedad.ALQUILADO, EstadoPropiedad.INACTIVO);
    }

    public static Optional<Moneda> normalizarMoneda(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String v = valor.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(Moneda.values()).filter(m -> m.name().equals(v)).findFirst();
    }

    private static Optional<String> coincidir(String valor, String... opciones) {
        if (valor == null) {
            return Optional.empty();
        }
        String v = valor.trim();
        return Arrays.stream(opciones).filter(o -> o.equalsIgnoreCase(v)).findFirst();
    }
}

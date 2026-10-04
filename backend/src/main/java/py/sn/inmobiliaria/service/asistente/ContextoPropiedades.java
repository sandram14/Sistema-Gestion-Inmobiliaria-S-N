package py.sn.inmobiliaria.service.asistente;

import py.sn.inmobiliaria.model.Casa;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.OperacionPropiedad;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Terreno;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Convierte propiedades en texto para el contexto de Gemini. Solo usa datos públicos
 * (los mismos que ya muestra la web): nunca datos del panel ni de usuarios.
 *
 * <p>Los textos se "limpian" antes de incluirlos: se quitan los delimitadores del prompt y la
 * marca de agente, y se recortan las descripciones largas, para que el contenido de una
 * propiedad no pueda alterar las instrucciones del asistente.</p>
 */
public final class ContextoPropiedades {

    static final int MAX_DESCRIPCION = 500;

    private ContextoPropiedades() {
    }

    /** Línea compacta para preguntas generales (muchas propiedades). */
    public static String resumen(Propiedad p) {
        return String.join(" | ",
                p.getCodigo(),
                p.getTipo().getNombre(),
                limpiar(p.getTitulo()),
                ubicacion(p),
                operaciones(p),
                medidas(p));
    }

    /** Ficha completa para preguntas sobre una propiedad específica. */
    public static String detalle(Propiedad p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Código: ").append(p.getCodigo()).append('\n');
        sb.append("Tipo: ").append(p.getTipo().getNombre()).append('\n');
        sb.append("Título: ").append(limpiar(p.getTitulo())).append('\n');
        sb.append("Ubicación: ").append(ubicacion(p)).append('\n');
        if (p.getDireccion() != null && !p.getDireccion().isBlank()) {
            sb.append("Dirección: ").append(limpiar(p.getDireccion())).append('\n');
        }
        sb.append("Operaciones y precios: ").append(operaciones(p)).append('\n');
        sb.append("Medidas: ").append(medidas(p)).append('\n');
        Casa c = p.getCasa();
        if (c != null) {
            sb.append("Ambientes: sala ").append(siNo(c.isSala())).append(", comedor ").append(siNo(c.isComedor()))
                    .append(", cocina ").append(siNo(c.isCocina())).append('\n');
            if (c.getSuperficieTerreno() != null) {
                sb.append("Superficie del terreno: ").append(numero(c.getSuperficieTerreno())).append(" m²\n");
            }
        }
        Terreno t = p.getTerreno();
        if (t != null) {
            if (t.getFrente() != null) {
                sb.append("Frente: ").append(numero(t.getFrente())).append(" m\n");
            }
            if (t.getFondo() != null) {
                sb.append("Fondo: ").append(numero(t.getFondo())).append(" m\n");
            }
            sb.append("Título de propiedad: ").append(siNo(t.isTieneTitulo())).append('\n');
        }
        long fotos = p.getImagenes().stream().map(Imagen::getUrl).count();
        sb.append("Fotos publicadas: ").append(fotos).append('\n');
        if (p.getDescripcion() != null && !p.getDescripcion().isBlank()) {
            String d = limpiar(p.getDescripcion());
            sb.append("Descripción: ").append(d.length() > MAX_DESCRIPCION ? d.substring(0, MAX_DESCRIPCION) + "…" : d)
                    .append('\n');
        }
        sb.append("Información no registrada (cochera, piscina, antigüedad, expensas, etc.): no disponible");
        return sb.toString();
    }

    private static String ubicacion(Propiedad p) {
        return limpiar(p.getBarrio().getCiudad().getNombre()) + ", barrio " + limpiar(p.getBarrio().getNombre())
                + " (" + limpiar(p.getBarrio().getCiudad().getDepartamento().getNombre()) + ")";
    }

    private static String operaciones(Propiedad p) {
        return p.getOperaciones().stream()
                .sorted(Comparator.comparing((OperacionPropiedad op) -> !Operacion.VENTA.equals(op.getOperacion().getNombre())))
                .map(op -> op.getOperacion().getNombre() + " " + numero(op.getPrecio()) + " " + op.getMoneda().name())
                .collect(Collectors.joining("; "));
    }

    private static String medidas(Propiedad p) {
        Casa c = p.getCasa();
        if (c != null) {
            return cantidad(c.getHabitaciones(), "habitación", "habitaciones") + ", "
                    + cantidad(c.getBanos(), "baño", "baños") + ", "
                    + numero(c.getSuperficieConstruida()) + " m² construidos";
        }
        Terreno t = p.getTerreno();
        return t == null ? "sin datos" : numero(t.getSuperficie()) + " m² de terreno";
    }

    /** "1 baño", "2 baños": evita que el asistente repita errores de concordancia del contexto. */
    private static String cantidad(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static String siNo(boolean valor) {
        return valor ? "sí" : "no";
    }

    private static String numero(BigDecimal valor) {
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return (sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros).toPlainString();
    }

    /**
     * Quita saltos de línea, delimitadores del prompt (&lt;&lt;&lt; &gt;&gt;&gt;) y la marca de agente,
     * para que un texto cargado en la base no pueda "salirse" de la sección de datos.
     */
    static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("<<<", " ").replace(">>>", " ")
                .replace(AsistenteService.MARCA_AGENTE, " ")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll(" {2,}", " ")
                .strip();
    }
}

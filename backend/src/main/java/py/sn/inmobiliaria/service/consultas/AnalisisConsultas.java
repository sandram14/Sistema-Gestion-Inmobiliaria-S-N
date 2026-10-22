package py.sn.inmobiliaria.service.consultas;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indicadores de las consultas del chat IA, calculados SOLO con datos guardados.
 *
 * <p>Cada indicador es "comprobado" (sale de un dato registrado) o "inferido" (interpretación
 * del texto por palabras clave). El resultado lo indica para que el panel lo muestre.</p>
 */
public final class AnalisisConsultas {

    private AnalisisConsultas() {
    }

    /** Lo que se analiza de cada consulta (sin datos personales). */
    /** precios: operaciones de la propiedad consultada (vacío si es una consulta general). */
    public record Fila(long id, java.time.OffsetDateTime fecha, String sesion, String pregunta, String respuesta,
                       Ref propiedad, String tipo, String ciudad, String barrio, String resultado, List<Ref> recomendadas,
                       List<Precio> precios) {
    }

    public record Precio(String operacion, java.math.BigDecimal monto, String moneda) {
    }

    /** Cantidad de propiedades consultadas en un rango de precio, por operación (en guaraníes). */
    public record Rango(String operacion, String moneda, String rango, long cantidad) {
    }

    /** Indicadores de calidad de las respuestas. */
    public record Calidad(List<Long> sinInformacionIds, List<Long> fueraDeAmbitoIds, long fueraDeAmbitoNoRegistrado,
                          List<Long> fallosServicioIds) {
    }

    public record Ref(long id, String codigo, String titulo) {
    }

    public record Conteo(String nombre, long cantidad) {
    }

    public record ConteoPropiedad(Ref propiedad, long cantidad) {
    }

    public record Resultado(long consultas, long conversaciones, long conPropiedad, long sinServicio,
                            long conRegistroCompleto, List<Conteo> preguntasRepetidas, List<ConteoPropiedad> propiedadesConsultadas,
                            List<Conteo> tiposComprobados, List<Conteo> zonasComprobadas, List<Conteo> temasInferidos,
                            List<Conteo> tiposInferidos, List<Conteo> zonasInferidas, List<ConteoPropiedad> masRecomendadas,
                            List<Long> sinServicioIds, List<Long> derivadasIds, List<Long> sinInformacionInferidaIds,
                            List<Rango> rangosPrecio, List<Conteo> caracteristicasInferidas, Calidad calidad) {
    }

    /** Características pedidas, por palabras clave (inferido). */
    static final Map<String, List<String>> CARACTERISTICAS = new LinkedHashMap<>();
    static {
        CARACTERISTICAS.put("Cochera", List.of("cochera", "garaje", "garage", "estacionamiento"));
        CARACTERISTICAS.put("Patio o jardín", List.of("patio", "jardin"));
        CARACTERISTICAS.put("Piscina", List.of("piscina", "pileta"));
        CARACTERISTICAS.put("Quincho", List.of("quincho", "parrilla"));
        CARACTERISTICAS.put("Cantidad de habitaciones", List.of("habitacion", "dormitorio", "pieza"));
        CARACTERISTICAS.put("Baños", List.of("bano"));
        CARACTERISTICAS.put("Amueblado", List.of("amueblad", "muebles"));
        CARACTERISTICAS.put("Superficie", List.of("metros", "m2", "superficie", "tamano"));
        CARACTERISTICAS.put("Seguridad", List.of("seguridad", "guardia", "cerco", "alarma"));
    }

    /** Límites de los rangos, en guaraníes (única moneda del sistema desde la migración 009). */
    static final Map<String, long[]> LIMITES = Map.of(
            "Venta PYG", new long[] {300_000_000, 700_000_000, 1_500_000_000},
            "Alquiler PYG", new long[] {2_000_000, 4_000_000, 7_000_000});

    /** Temas por palabras clave (sin acentos, en minúsculas). Es una inferencia: lo indica el panel. */
    static final Map<String, List<String>> TEMAS = new LinkedHashMap<>();
    static {
        TEMAS.put("Precio", List.of("precio", "cuesta", "costo", "cuanto", "valor", "usd", "dolar", "guarani", "presupuesto"));
        TEMAS.put("Ubicación", List.of("donde", "ubica", "zona", "barrio", "cerca", "direccion", "ciudad"));
        TEMAS.put("Características", List.of("habitacion", "dormitorio", "bano", "cochera", "garaje", "patio", "piscina",
                "metros", "m2", "superficie", "amueblad", "quincho"));
        TEMAS.put("Alquiler o venta", List.of("alquil", "venta", "vender", "comprar", "compra"));
        TEMAS.put("Visitas", List.of("visita", "visitar", "conocer", "ver la", "ver el", "cita"));
        TEMAS.put("Financiación", List.of("financ", "cuota", "credito", "hipotec", "entrega inicial"));
        TEMAS.put("Documentación", List.of("titulo", "escritura", "papeles", "documento"));
    }

    static final Map<String, List<String>> TIPOS = Map.of(
            "Casa", List.of("casa", "casas", "vivienda"),
            "Terreno", List.of("terreno", "terrenos", "lote", "lotes"));

    /** Frases con las que el asistente indica que no tiene la información (para consultas sin resultado registrado). */
    static final List<String> SIN_INFORMACION = List.of("no dispongo", "no tengo esa informacion", "no tengo informacion",
            "no cuento con", "no figura", "no se encuentra disponible", "no esta disponible en nuestros registros");

    /**
     * @param filas          consultas del periodo
     * @param zonas          nombres de ciudades y barrios del catálogo (para detectar zonas mencionadas)
     * @param respuestaRespaldo mensaje que se guarda cuando el asistente no pudo responder
     */
    public static Resultado calcular(List<Fila> filas, List<String> zonas, String respuestaRespaldo) {
        long conversaciones = filas.stream().map(Fila::sesion).distinct().count();
        long conPropiedad = filas.stream().filter(f -> f.propiedad() != null).count();
        List<Long> sinServicio = filas.stream().filter(f -> esSinServicio(f, respuestaRespaldo)).map(Fila::id).toList();
        List<Long> derivadas = filas.stream().filter(f -> "DERIVADA_ASESOR".equals(f.resultado())).map(Fila::id).toList();
        List<Long> sinInfo = filas.stream()
                .filter(f -> f.resultado() == null && !esSinServicio(f, respuestaRespaldo) && contieneAlguna(f.respuesta(), SIN_INFORMACION))
                .map(Fila::id).toList();

        // Preguntas repetidas: idénticas tras normalizar (comprobado). Solo las que aparecen 2 o más veces.
        Map<String, List<Fila>> porPregunta = filas.stream().collect(Collectors.groupingBy(f -> normalizar(f.pregunta()),
                LinkedHashMap::new, Collectors.toList()));
        List<Conteo> repetidas = porPregunta.values().stream().filter(l -> l.size() > 1)
                .map(l -> new Conteo(l.get(0).pregunta().strip(), l.size())).sorted(mayorPrimero()).limit(10).toList();

        List<Fila> conProp = filas.stream().filter(f -> f.propiedad() != null).toList();
        List<Fila> generales = filas.stream().filter(f -> f.propiedad() == null).toList();
        List<String> zonasNorm = zonas.stream().filter(z -> z != null && z.length() >= 3).distinct().toList();

        return new Resultado(filas.size(), conversaciones, conPropiedad, sinServicio.size(),
                filas.stream().filter(f -> f.resultado() != null).count(),
                repetidas,
                contarPropiedades(conProp.stream().map(Fila::propiedad).toList()),
                contar(conProp, Fila::tipo), contar(conProp, Fila::ciudad),
                contarMultiple(filas, f -> clasificar(f.pregunta(), TEMAS)),
                contarMultiple(generales, f -> clasificar(f.pregunta(), TIPOS)),
                contarMultiple(generales, f -> zonasNorm.stream().filter(z -> contienePalabra(f.pregunta(), z)).collect(Collectors.toSet())),
                contarPropiedades(filas.stream().flatMap(f -> f.recomendadas().stream()).toList()),
                sinServicio, derivadas, sinInfo,
                rangos(conProp),
                contarMultiple(filas, f -> clasificar(f.pregunta(), CARACTERISTICAS)),
                new Calidad(
                        // Sin información útil (inferido): el asistente dijo no tener el dato, en cualquier consulta
                        filas.stream().filter(f -> !esSinServicio(f, respuestaRespaldo) && !"FUERA_DE_AMBITO".equals(f.resultado())
                                && contieneAlguna(f.respuesta(), SIN_INFORMACION)).map(Fila::id).toList(),
                        // Fuera del ámbito (comprobado): solo con resultado registrado; las antiguas no se clasifican
                        filas.stream().filter(f -> "FUERA_DE_AMBITO".equals(f.resultado())).map(Fila::id).toList(),
                        filas.stream().filter(f -> f.resultado() == null).count(),
                        sinServicio));
    }

    /** Rangos de precio de las propiedades consultadas (una vez por consulta y operación). */
    static List<Rango> rangos(List<Fila> conPropiedad) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Fila f : conPropiedad) {
            for (Precio p : f.precios()) {
                long[] lim = LIMITES.get(p.operacion() + " " + p.moneda());
                if (lim == null || p.monto() == null) {
                    continue;
                }
                conteo.merge(p.operacion() + "|" + p.moneda() + "|" + rango(p.monto().longValue(), lim, p.moneda()), 1L, Long::sum);
            }
        }
        List<Rango> lista = new ArrayList<>();
        for (String op : List.of("Venta", "Alquiler")) {
            for (String mon : List.of("PYG")) {
                long[] lim = LIMITES.get(op + " " + mon);
                for (int i = 0; i <= lim.length; i++) {
                    String r = etiquetaRango(i, lim, mon);
                    Long n = conteo.get(op + "|" + mon + "|" + r);
                    if (n != null) {
                        lista.add(new Rango(op, mon, r, n));
                    }
                }
            }
        }
        return lista;
    }

    private static String rango(long monto, long[] lim, String moneda) {
        int i = 0;
        while (i < lim.length && monto >= lim[i]) {
            i++;
        }
        return etiquetaRango(i, lim, moneda);
    }

    static String etiquetaRango(int i, long[] lim, String moneda) {
        String simbolo = "PYG".equals(moneda) ? "Gs." : moneda;
        java.util.function.LongFunction<String> f = v -> simbolo + " " + String.format(java.util.Locale.ROOT, "%,d", v).replace(',', '.');
        if (i == 0) {
            return "Menos de " + f.apply(lim[0]);
        }
        if (i == lim.length) {
            return f.apply(lim[lim.length - 1]) + " o más";
        }
        return f.apply(lim[i - 1]) + " a " + f.apply(lim[i]);
    }

    static boolean esSinServicio(Fila f, String respaldo) {
        if (f.resultado() != null) {
            return "SIN_SERVICIO".equals(f.resultado()) || "BLOQUEADA".equals(f.resultado());
        }
        return f.respuesta() == null || f.respuesta().strip().equals(respaldo); // mensaje de respaldo exacto: comprobado
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sin = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sin.toLowerCase(Locale.ROOT).replaceAll("[¿?¡!.,;:]", " ").replaceAll("\\s+", " ").strip();
    }

    static Set<String> clasificar(String texto, Map<String, List<String>> categorias) {
        String t = normalizar(texto);
        return categorias.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(t::contains))
                .map(Map.Entry::getKey).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    static boolean contieneAlguna(String texto, List<String> frases) {
        String t = normalizar(texto);
        return frases.stream().anyMatch(t::contains);
    }

    static boolean contienePalabra(String texto, String palabra) {
        return (" " + normalizar(texto) + " ").contains(" " + normalizar(palabra) + " ");
    }

    private static List<Conteo> contar(List<Fila> filas, Function<Fila, String> clave) {
        return filas.stream().map(clave).filter(v -> v != null && !v.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream().map(e -> new Conteo(e.getKey(), e.getValue())).sorted(mayorPrimero()).toList();
    }

    private static List<Conteo> contarMultiple(List<Fila> filas, Function<Fila, Set<String>> claves) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Fila f : filas) {
            for (String k : claves.apply(f)) {
                m.merge(k, 1L, Long::sum);
            }
        }
        return m.entrySet().stream().map(e -> new Conteo(e.getKey(), e.getValue())).sorted(mayorPrimero()).toList();
    }

    private static List<ConteoPropiedad> contarPropiedades(List<Ref> refs) {
        Map<Long, Ref> porId = new LinkedHashMap<>();
        Map<Long, Long> cantidad = new LinkedHashMap<>();
        for (Ref r : refs) {
            porId.putIfAbsent(r.id(), r);
            cantidad.merge(r.id(), 1L, Long::sum);
        }
        List<ConteoPropiedad> lista = new ArrayList<>();
        cantidad.forEach((id, n) -> lista.add(new ConteoPropiedad(porId.get(id), n)));
        lista.sort(Comparator.comparingLong(ConteoPropiedad::cantidad).reversed());
        return lista.stream().limit(10).toList();
    }

    private static Comparator<Conteo> mayorPrimero() {
        return Comparator.comparingLong(Conteo::cantidad).reversed().thenComparing(Conteo::nombre);
    }
}

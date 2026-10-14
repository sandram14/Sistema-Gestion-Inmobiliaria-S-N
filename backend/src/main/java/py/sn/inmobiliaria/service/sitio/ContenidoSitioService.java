package py.sn.inmobiliaria.service.sitio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.util.JsonUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Contenido editable del sitio (Gestión del sitio web). Cada sección tiene campos fijos con su validación:
 * no se pueden guardar campos que no existan. Los cambios se publican al guardar.
 */
public class ContenidoSitioService {

    /** Carpeta de las imágenes del sitio dentro de /uploads (cumple la regla de carpetas de AlmacenImagenes). */
    public static final String CARPETA_IMAGENES = "sitio-001";
    public static final int MAXIMO_DESTACADAS = 12;
    public static final List<String> EDITABLES = List.of("INICIO", "SERVICIOS", "NOSOTROS", "CONTACTO");
    private static final List<String> SECCIONES = List.of("INICIO", "SERVICIOS", "NOSOTROS", "CONTACTO");
    private static final Pattern IMAGEN = Pattern.compile("^/uploads/" + CARPETA_IMAGENES + "/[0-9a-f-]{36}\\.(jpg|png|webp)$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9 ()-]{6,30}$");
    private static final Pattern WHATSAPP = Pattern.compile("^[0-9]{8,15}$");

    /** Portada. Se retiraron el bloque "Sobre nosotros" y el texto pequeño sobre el título: ya no se editan ni se aceptan. */
    public record Inicio(String imagenPortada, String titulo, String subtitulo) {
    }

    public record Item(String titulo, String texto) {
    }

    public record Servicios(String titulo, String subtitulo, List<Item> servicios, String bloqueTitulo, String bloqueTexto) {
    }

    public record Cifra(String valor, String etiqueta) {
    }

    public record Nosotros(String introduccion, String historiaTitulo, String historiaTexto, String imagen,
                           List<Cifra> cifras, List<Item> valores, List<Item> razones) {
    }

    public record Contacto(String nombreEmpresa, String descripcion, String direccion, String ciudad, String email,
                           String telefono, String whatsapp, String horario) {
    }

    private final ContenidoSitioDao dao;
    private final ObjectMapper json = JsonUtil.mapper();

    public ContenidoSitioService(ContenidoSitioDao dao) {
        this.dao = dao;
    }

    static String seccion(String nombre) {
        String s = nombre == null ? "" : nombre.strip().toUpperCase(Locale.ROOT);
        if (!SECCIONES.contains(s)) {
            throw new NoEncontradoException("La sección no existe");
        }
        return s;
    }

    public JsonNode leer(String nombre) {
        String s = seccion(nombre);
        return arbol(dao.leer(s).orElseThrow(() -> new NoEncontradoException("La sección no tiene contenido (¿falta la migración 006?)")));
    }

    /** Valida y guarda. Devuelve el contenido normalizado (sin espacios sobrantes). */
    public JsonNode guardar(String nombre, JsonNode cuerpo, int idUsuario) {
        String s = seccion(nombre);
        if (!EDITABLES.contains(s)) {
            throw new NoEncontradoException("Esta sección todavía no se puede editar desde el panel");
        }
        Object normalizado = switch (s) {
            case "INICIO" -> validarInicio(convertir(cuerpo, Inicio.class));
            case "SERVICIOS" -> validarServicios(convertir(cuerpo, Servicios.class));
            case "NOSOTROS" -> validarNosotros(convertir(cuerpo, Nosotros.class));
            case "CONTACTO" -> validarContacto(convertir(cuerpo, Contacto.class));
            default -> throw new IllegalStateException(s);
        };
        String texto = escribir(normalizado);
        dao.guardar(s, texto, idUsuario);
        return arbol(texto);
    }

    static Inicio validarInicio(Inicio i) {
        Errores e = new Errores();
        Inicio n = new Inicio(imagen(i.imagenPortada(), "imagenPortada", e),
                obligatorio(i.titulo(), "titulo", 120, e), opcional(i.subtitulo(), "subtitulo", 300, e));
        e.lanzarSiHay();
        return n;
    }

    static Servicios validarServicios(Servicios s) {
        Errores e = new Errores();
        Servicios n = new Servicios(obligatorio(s.titulo(), "titulo", 80, e), opcional(s.subtitulo(), "subtitulo", 200, e),
                items(s.servicios(), "servicios", 1, 8, e), opcional(s.bloqueTitulo(), "bloqueTitulo", 120, e),
                opcional(s.bloqueTexto(), "bloqueTexto", 600, e));
        e.lanzarSiHay();
        return n;
    }

    static Nosotros validarNosotros(Nosotros x) {
        Errores e = new Errores();
        List<Cifra> cifras = x.cifras() == null ? List.of() : x.cifras();
        if (cifras.size() > 4) e.agregar("cifras", "Como máximo 4 cifras");
        List<Cifra> cifrasN = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(cifras.size(), 4); i++) {
            Cifra c = cifras.get(i) == null ? new Cifra(null, null) : cifras.get(i);
            cifrasN.add(new Cifra(obligatorio(c.valor(), "cifras[" + i + "].valor", 12, e), obligatorio(c.etiqueta(), "cifras[" + i + "].etiqueta", 40, e)));
        }
        Nosotros n = new Nosotros(opcional(x.introduccion(), "introduccion", 600, e), opcional(x.historiaTitulo(), "historiaTitulo", 120, e),
                opcional(x.historiaTexto(), "historiaTexto", 1500, e), imagen(x.imagen(), "imagen", e), cifrasN,
                items(x.valores(), "valores", 1, 6, e), items(x.razones(), "razones", 1, 6, e));
        e.lanzarSiHay();
        return n;
    }

    /** Lista de elementos con título y texto obligatorios, entre min y max (para no romper el diseño). */
    private static List<Item> items(List<Item> lista, String campo, int min, int max, Errores e) {
        List<Item> l = lista == null ? List.of() : lista;
        if (l.size() < min || l.size() > max) {
            e.agregar(campo, "Debe haber entre " + min + " y " + max + " elementos");
        }
        List<Item> n = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(l.size(), max); i++) {
            Item it = l.get(i) == null ? new Item(null, null) : l.get(i);
            n.add(new Item(obligatorio(it.titulo(), campo + "[" + i + "].titulo", 60, e), obligatorio(it.texto(), campo + "[" + i + "].texto", 300, e)));
        }
        return n;
    }

    static Contacto validarContacto(Contacto c) {
        Errores e = new Errores();
        String email = obligatorio(c.email(), "email", 120, e);
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) e.agregar("email", "Ingrese un email válido");
        String telefono = obligatorio(c.telefono(), "telefono", 30, e);
        if (!telefono.isEmpty() && !TELEFONO.matcher(telefono).matches()) e.agregar("telefono", "Ingrese un teléfono válido");
        // Primero se quitan espacios, "+", guiones y paréntesis; después se valida (así "+595 981 123-456" es válido)
        String whatsapp = obligatorio(c.whatsapp() == null ? null : c.whatsapp().replaceAll("[ +()-]", ""), "whatsapp", 15, e);
        if (!whatsapp.isEmpty() && !WHATSAPP.matcher(whatsapp).matches()) {
            e.agregar("whatsapp", "Use el formato internacional, solo números (por ejemplo 595981123456)");
        }
        Contacto n = new Contacto(obligatorio(c.nombreEmpresa(), "nombreEmpresa", 120, e), opcional(c.descripcion(), "descripcion", 400, e),
                opcional(c.direccion(), "direccion", 200, e), opcional(c.ciudad(), "ciudad", 100, e), email, telefono, whatsapp,
                opcional(c.horario(), "horario", 200, e));
        e.lanzarSiHay();
        return n;
    }

    /** true si algún dato de contacto sigue siendo el provisional de la instalación. */
    public static boolean esProvisional(JsonNode contacto) {
        String email = contacto.path("email").asText("");
        String tel = contacto.path("telefono").asText("").replaceAll("[^0-9]", "");
        String wa = contacto.path("whatsapp").asText("");
        return email.endsWith(".example") || tel.endsWith("000000000") || wa.endsWith("000000000");
    }

    // ---- Propiedades destacadas ----

    public List<ContenidoSitioDao.Destacada> destacadas() {
        return dao.destacadas();
    }

    /** Reemplaza la selección: sin repetidas, como máximo 12 y todas publicadas y disponibles. */
    public List<ContenidoSitioDao.Destacada> guardarDestacadas(List<Long> ids) {
        if (ids == null) {
            throw ValidacionException.deCampo("ids", "Indique la lista de propiedades (puede estar vacía)");
        }
        if (new LinkedHashSet<>(ids).size() != ids.size()) {
            throw ValidacionException.deCampo("ids", "Hay propiedades repetidas");
        }
        if (ids.size() > MAXIMO_DESTACADAS) {
            throw ValidacionException.deCampo("ids", "Como máximo " + MAXIMO_DESTACADAS + " propiedades destacadas");
        }
        List<Long> disponibles = dao.disponibles(ids);
        if (disponibles.size() != ids.size()) {
            throw ValidacionException.deCampo("ids", "Solo se pueden destacar propiedades publicadas y disponibles");
        }
        dao.reemplazarDestacadas(ids);
        return dao.destacadas();
    }

    /** Contenido publicable para el sitio: las 4 secciones, la marca de datos provisionales y las ofertas disponibles. */
    public ObjectNode publico() {
        ObjectNode r = json.createObjectNode();
        for (String s : SECCIONES) {
            dao.leer(s).ifPresent(t -> r.set(s.toLowerCase(Locale.ROOT), arbol(t)));
        }
        r.put("contactoProvisional", r.has("contacto") && esProvisional(r.get("contacto")));
        r.set("ofertas", json.valueToTree(dao.codigosDestacadosDisponibles()));
        return r;
    }

    // ---- Apoyo ----

    private static String obligatorio(String v, String campo, int max, Errores e) {
        String t = v == null ? "" : v.strip();
        if (t.isEmpty()) e.agregar(campo, "Es obligatorio");
        else if (t.length() > max) e.agregar(campo, "Máximo " + max + " caracteres");
        return t;
    }

    private static String opcional(String v, String campo, int max, Errores e) {
        String t = v == null ? "" : v.strip();
        if (t.length() > max) e.agregar(campo, "Máximo " + max + " caracteres");
        return t;
    }

    /** Solo imágenes subidas desde el panel a la carpeta del sitio (o vacío: imagen del diseño). */
    private static String imagen(String v, String campo, Errores e) {
        if (v == null || v.isBlank()) return null;
        if (!IMAGEN.matcher(v.strip()).matches()) e.agregar(campo, "Use una imagen subida desde el panel");
        return v.strip();
    }

    private <T> T convertir(JsonNode cuerpo, Class<T> tipo) {
        try {
            return json.treeToValue(cuerpo, tipo);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw ValidacionException.deCampo("contenido", "El contenido no tiene el formato esperado para esta sección");
        }
    }

    private String escribir(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode arbol(String t) {
        try {
            return json.readTree(t);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Contenido del sitio dañado", e);
        }
    }
}

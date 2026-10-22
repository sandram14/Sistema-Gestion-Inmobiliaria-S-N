package py.sn.inmobiliaria.service.consultas;

import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.response.admin.ConsultaDtos;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Consulta;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.repository.ConsultaAdminDao;
import py.sn.inmobiliaria.seguridad.Permiso;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.asistente.AsistenteService;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Historial, revisión interna y análisis de las consultas del chat IA.
 * El vendedor solo ve las consultas generales y las de sus propiedades (consultadas o recomendadas):
 * el filtro se aplica en la consulta a la base. Una consulta no visible responde 404, sin revelar que existe.
 */
public class ConsultaAdminService {

    public static final int TAMANO_PAGINA = 20;
    public static final int LIMITE_ANALISIS = 5000;
    private static final int LARGO_RESUMEN = 160;

    private final TxRunner tx;
    private final ConsultaAdminDao dao;
    private final Supplier<List<String>> zonas; // nombres de ciudades y barrios del catálogo

    public ConsultaAdminService(TxRunner tx, ConsultaAdminDao dao, Supplier<List<String>> zonas) {
        this.tx = tx;
        this.dao = dao;
        this.zonas = zonas;
    }

    /** null si el usuario ve todas; su id si es un vendedor (solo las visibles para él). */
    static Integer alcance(UsuarioSesion actor) {
        return actor.tiene(Permiso.CONSULTAS_VER_TODAS) ? null : actor.id();
    }

    /** @param pagina desde 1 */
    public ConsultaDtos.Pagina historial(String texto, OffsetDateTime desde, OffsetDateTime hasta, String estado,
                                         String resultado, int pagina, UsuarioSesion actor) {
        if (pagina < 1) {
            throw ValidacionException.deCampo("pagina", "Debe ser 1 o mayor");
        }
        var filtro = new ConsultaAdminDao.Filtro(limpio(texto), desde, hasta, validarEstado(estado), validarResultado(resultado),
                alcance(actor), TAMANO_PAGINA);
        return tx.soloLectura(em -> new ConsultaDtos.Pagina(
                dao.buscar(em, filtro, (pagina - 1) * TAMANO_PAGINA).stream().map(ConsultaAdminService::resumen).toList(),
                dao.contar(em, filtro), pagina, TAMANO_PAGINA));
    }

    public ConsultaDtos.Detalle detalle(long id, UsuarioSesion actor) {
        return tx.soloLectura(em -> {
            Consulta c = dao.porId(em, id, alcance(actor)).orElseThrow(() -> new NoEncontradoException("La consulta no existe"));
            List<ConsultaDtos.ItemConversacion> conversacion = dao.deSesion(em, c.getIdSesion(), alcance(actor)).stream()
                    .map(o -> new ConsultaDtos.ItemConversacion(o.getId(), o.getFechaHora(), resumir(o.getConsulta()), o.getId().equals(c.getId())))
                    .toList();
            Usuario revisor = c.getRevisadaPor();
            return new ConsultaDtos.Detalle(c.getId(), c.getFechaHora(), c.getConsulta(), c.getRespuesta(), ref(c.getPropiedad()),
                    c.getRecomendadas().stream().sorted(Comparator.comparing(Propiedad::getCodigo)).map(ConsultaAdminService::ref).toList(),
                    c.getResultado(), c.getResultado() != null, c.getEstadoRevision(),
                    revisor == null ? null : revisor.getNombre() + " " + revisor.getApellido(), c.getRevisadaEn(), conversacion);
        });
    }

    /** Revisión INTERNA: indica si alguien del equipo ya leyó la conversación. */
    public ConsultaDtos.Detalle revisar(long id, String estado, UsuarioSesion actor) {
        String e = validarEstado(estado);
        if (e == null) {
            throw ValidacionException.deCampo("estado", "Indique PENDIENTE o REVISADA");
        }
        tx.enTransaccion(em -> {
            Consulta c = dao.porId(em, id, alcance(actor)).orElseThrow(() -> new NoEncontradoException("La consulta no existe"));
            c.revisar("REVISADA".equals(e), em.find(Usuario.class, actor.id()), OffsetDateTime.now());
            return null;
        });
        return detalle(id, actor);
    }

    /** Análisis de todo el sitio (permiso CONSULTAS_VER_TODAS, lo exige AuthFilter). */
    public ConsultaDtos.Analisis analisis(OffsetDateTime desde, OffsetDateTime hasta) {
        List<String> nombresZonas = zonas.get();
        return tx.soloLectura(em -> {
            List<Consulta> consultas = dao.buscar(em, new ConsultaAdminDao.Filtro(null, desde, hasta, null, null, null, LIMITE_ANALISIS));
            List<AnalisisConsultas.Fila> filas = consultas.stream().map(ConsultaAdminService::fila).toList();
            var r = AnalisisConsultas.calcular(filas, nombresZonas, AsistenteService.RESPUESTA_RESPALDO_PUBLICA);
            Map<Long, ConsultaDtos.Resumen> porId = consultas.stream().collect(Collectors.toMap(Consulta::getId, ConsultaAdminService::resumen));
            Function<List<Long>, List<ConsultaDtos.Resumen>> lista = ids -> ids.stream().map(porId::get).toList();
            return new ConsultaDtos.Analisis(desde, hasta, r, lista.apply(r.sinServicioIds()), lista.apply(r.derivadasIds()),
                    lista.apply(r.sinInformacionInferidaIds()), lista.apply(r.calidad().sinInformacionIds()),
                    lista.apply(r.calidad().fueraDeAmbitoIds()));
        });
    }

    static AnalisisConsultas.Fila fila(Consulta c) {
        Propiedad p = c.getPropiedad();
        return new AnalisisConsultas.Fila(c.getId(), c.getFechaHora(), c.getIdSesion().toString(), c.getConsulta(), c.getRespuesta(),
                p == null ? null : new AnalisisConsultas.Ref(p.getId(), p.getCodigo(), p.getTitulo()),
                p == null ? null : p.getTipo().getNombre(), p == null ? null : p.getBarrio().getCiudad().getNombre(),
                p == null ? null : p.getBarrio().getNombre(), c.getResultado(),
                c.getRecomendadas().stream().map(r -> new AnalisisConsultas.Ref(r.getId(), r.getCodigo(), r.getTitulo())).toList(),
                p == null ? List.of() : p.getOperaciones().stream()
                        .map(o -> new AnalisisConsultas.Precio(o.getOperacion().getNombre(), o.getPrecio(), o.getMoneda().name())).toList());
    }

    static ConsultaDtos.Resumen resumen(Consulta c) {
        return new ConsultaDtos.Resumen(c.getId(), c.getFechaHora(), resumir(c.getConsulta()), ref(c.getPropiedad()),
                c.getResultado(), c.getEstadoRevision(), c.getResultado() != null);
    }

    static ConsultaDtos.PropiedadRef ref(Propiedad p) {
        return p == null ? null : new ConsultaDtos.PropiedadRef(p.getId(), p.getCodigo(), p.getTitulo());
    }

    static String resumir(String texto) {
        String t = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
        return t.length() <= LARGO_RESUMEN ? t : t.substring(0, LARGO_RESUMEN - 1) + "…";
    }

    private static String limpio(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        if (texto.length() > 100) {
            throw ValidacionException.deCampo("texto", "Máximo 100 caracteres");
        }
        return texto.strip();
    }

    private static String validarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        String e = estado.strip().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("PENDIENTE", "REVISADA").contains(e)) {
            throw ValidacionException.deCampo("estado", "Indique PENDIENTE o REVISADA");
        }
        return e;
    }

    private static String validarResultado(String resultado) {
        if (resultado == null || resultado.isBlank()) {
            return null;
        }
        String r = resultado.strip().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("RESPONDIDA", "DERIVADA_ASESOR", "SIN_SERVICIO", "BLOQUEADA", "FUERA_DE_AMBITO", "NO_REGISTRADO").contains(r)) {
            throw ValidacionException.deCampo("resultado", "Resultado no válido");
        }
        return r;
    }
}

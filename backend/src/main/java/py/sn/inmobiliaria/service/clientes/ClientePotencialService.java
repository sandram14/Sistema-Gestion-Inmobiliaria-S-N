package py.sn.inmobiliaria.service.clientes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.ClienteDtos;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.ClientePotencial;
import py.sn.inmobiliaria.model.Consulta;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Usuario;
import py.sn.inmobiliaria.seguridad.Permiso;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;
import py.sn.inmobiliaria.service.validacion.Errores;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Clientes potenciales: registro desde el chat (con aceptación del tratamiento) y gestión en el panel.
 * El vendedor solo ve y actualiza los asignados a él; un cliente no visible responde 404.
 */
public class ClientePotencialService {

    /** Versión vigente del texto de aceptación que muestra el formulario público (ver docs/integracion-clientes-potenciales.md). */
    public static final String VERSION_CONSENTIMIENTO = "2026-10";
    public static final int TAMANO_PAGINA = 20;
    static final Set<String> ESTADOS = Set.of("PENDIENTE", "CONTACTADO", "DESCARTADO");
    static final Set<String> ORIGENES_MANUALES = Set.of("WHATSAPP", "LLAMADA", "OTRO");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9 ()-]{6,30}$");

    private final TxRunner tx;

    public ClientePotencialService(TxRunner tx) {
        this.tx = tx;
    }

    record Datos(String nombre, String telefono, String email, String interes) {
    }

    /** Validación común (también la usan las pruebas). */
    static Datos validar(String nombre, String telefono, String email, String interes, Errores e) {
        String n = nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ");
        if (n.length() < 2 || n.length() > 120) {
            e.agregar("nombre", "Ingrese un nombre de 2 a 120 caracteres");
        }
        String t = telefono == null || telefono.isBlank() ? null : telefono.strip();
        String m = email == null || email.isBlank() ? null : email.strip();
        if (t == null && m == null) {
            e.agregar("contacto", "Indique un teléfono o un email para que podamos contactarle");
        }
        if (t != null && !TELEFONO.matcher(t).matches()) {
            e.agregar("telefono", "Ingrese un teléfono válido");
        }
        if (m != null && (m.length() > 120 || !EMAIL.matcher(m).matches())) {
            e.agregar("email", "Ingrese un email válido");
        }
        String i = interes == null || interes.isBlank() ? null : interes.strip();
        if (i != null && i.length() > 1000) {
            e.agregar("interes", "Máximo 1000 caracteres");
        }
        return new Datos(n, t, m, i);
    }

    // ---- Registro público desde el chat ----

    /** Registra al interesado (chat o formulario de Contacto). Desde el chat, vincula su última consulta (misma sesión, últimas 2 horas). */
    public void registrarDesdeChat(ClienteDtos.Solicitud s, UUID sesion) {
        Errores e = new Errores();
        Datos d = validar(s == null ? null : s.nombre(), s == null ? null : s.telefono(), s == null ? null : s.email(),
                s == null ? null : s.mensaje(), e);
        if (s == null || !Boolean.TRUE.equals(s.aceptaTratamiento())) {
            e.agregar("aceptaTratamiento", "Debe aceptar el uso de sus datos para ser contactado");
        } else if (!VERSION_CONSENTIMIENTO.equals(s.versionConsentimiento())) {
            e.agregar("versionConsentimiento", "La versión del texto de aceptación no es la vigente");
        }
        String canal = s == null || s.canal() == null || s.canal().isBlank() ? "CHAT" : s.canal().strip().toUpperCase(Locale.ROOT);
        if (!Set.of("CHAT", "CONTACTO").contains(canal)) {
            e.agregar("canal", "Use CHAT o CONTACTO");
        }
        e.lanzarSiHay();
        tx.enTransaccion(em -> {
            ClientePotencial c = new ClientePotencial(d.nombre(), d.telefono(), d.email(), d.interes(), canal);
            c.registrarConsentimiento(OffsetDateTime.now(), VERSION_CONSENTIMIENTO);
            Propiedad p = propiedad(em, s.codigoPropiedad());
            c.setPropiedad(p);
            c.setResponsable(responsableDe(p));
            if (sesion != null && "CHAT".equals(canal)) { // solo el chat tiene una consulta previa que vincular
                em.createQuery("select c from Consulta c where c.idSesion = :s and c.fechaHora >= :desde order by c.fechaHora desc", Consulta.class)
                        .setParameter("s", sesion).setParameter("desde", OffsetDateTime.now().minusHours(2))
                        .setMaxResults(1).getResultStream().findFirst().ifPresent(c::setConsulta);
            }
            em.persist(c);
            return null;
        });
    }

    // ---- Panel ----

    static Integer alcance(UsuarioSesion actor) {
        return actor.tiene(Permiso.CLIENTES_VER_TODOS) ? null : actor.id();
    }

    public ClienteDtos.Pagina listar(String texto, String estado, String origen, int pagina, UsuarioSesion actor) {
        if (pagina < 1) {
            throw ValidacionException.deCampo("pagina", "Debe ser 1 o mayor");
        }
        String est = opcionEn(estado, ESTADOS, "estado");
        String ori = opcionEn(origen, Set.of("CHAT", "CONTACTO", "WHATSAPP", "LLAMADA", "OTRO"), "origen");
        Integer vendedor = alcance(actor);
        return tx.soloLectura(em -> {
            Map<String, Object> params = new HashMap<>();
            StringBuilder w = new StringBuilder(" where 1 = 1");
            if (vendedor != null) { w.append(" and c.responsable.id = :yo"); params.put("yo", vendedor); }
            if (texto != null && !texto.isBlank()) {
                w.append(" and (lower(c.nombre) like :t or lower(coalesce(c.telefono, '')) like :t or lower(coalesce(c.email, '')) like :t"
                        + " or lower(coalesce(c.interes, '')) like :t)");
                params.put("t", "%" + texto.strip().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%");
            }
            if (ori != null) { w.append(" and c.origen = :o"); params.put("o", ori); }
            // Conteos por estado (sin el filtro de estado, para las fichas)
            TypedQuery<Object[]> qc = em.createQuery("select c.estado, count(c) from ClientePotencial c" + w + " group by c.estado", Object[].class);
            params.forEach(qc::setParameter);
            Map<String, Long> porEstado = new HashMap<>();
            for (Object[] f : qc.getResultList()) {
                porEstado.put((String) f[0], (Long) f[1]);
            }
            if (est != null) { w.append(" and c.estado = :e"); params.put("e", est); }
            TypedQuery<ClientePotencial> q = em.createQuery("select c from ClientePotencial c left join fetch c.propiedad left join fetch c.responsable"
                    + w + " order by c.creadoEn desc, c.id desc", ClientePotencial.class)
                    .setFirstResult((pagina - 1) * TAMANO_PAGINA).setMaxResults(TAMANO_PAGINA);
            params.forEach(q::setParameter);
            TypedQuery<Long> qt = em.createQuery("select count(c) from ClientePotencial c" + w, Long.class);
            params.forEach(qt::setParameter);
            long pend = porEstado.getOrDefault("PENDIENTE", 0L), cont = porEstado.getOrDefault("CONTACTADO", 0L),
                    desc = porEstado.getOrDefault("DESCARTADO", 0L);
            return new ClienteDtos.Pagina(q.getResultList().stream().map(ClientePotencialService::resumen).toList(),
                    qt.getSingleResult(), pagina, TAMANO_PAGINA, new ClienteDtos.Conteos(pend + cont + desc, pend, cont, desc));
        });
    }

    public ClienteDtos.Detalle obtener(long id, UsuarioSesion actor) {
        return tx.soloLectura(em -> detalle(buscar(em, id, actor)));
    }

    /** Alta manual (contactos por WhatsApp, llamada u otro medio). El vendedor queda como responsable. */
    public ClienteDtos.Detalle registrarManual(ClienteDtos.Manual m, UsuarioSesion actor) {
        Errores e = new Errores();
        Datos d = validar(m == null ? null : m.nombre(), m == null ? null : m.telefono(), m == null ? null : m.email(),
                m == null ? null : m.interes(), e);
        String origen = m == null || m.origen() == null ? null : m.origen().strip().toUpperCase(Locale.ROOT);
        if (origen == null || !ORIGENES_MANUALES.contains(origen)) {
            e.agregar("origen", "Indique el medio: WhatsApp, llamada u otro");
        }
        e.lanzarSiHay();
        return tx.enTransaccion(em -> {
            ClientePotencial c = new ClientePotencial(d.nombre(), d.telefono(), d.email(), d.interes(), origen);
            Propiedad p = propiedad(em, m.codigoPropiedad());
            c.setPropiedad(p);
            c.setRegistradoPor(em.find(Usuario.class, actor.id()));
            if (actor.tiene(Permiso.CLIENTES_ASIGNAR)) {
                c.setResponsable(m.idResponsable() != null ? usuarioActivo(em, m.idResponsable()) : responsableDe(p));
            } else {
                c.setResponsable(em.find(Usuario.class, actor.id()));
            }
            em.persist(c);
            return detalle(c);
        });
    }

    public ClienteDtos.Detalle cambiarEstado(long id, String estado, UsuarioSesion actor) {
        String e = opcionEn(estado, ESTADOS, "estado");
        if (e == null) {
            throw ValidacionException.deCampo("estado", "Indique PENDIENTE, CONTACTADO o DESCARTADO");
        }
        return tx.enTransaccion(em -> {
            ClientePotencial c = buscar(em, id, actor);
            c.setEstado(e);
            return detalle(c);
        });
    }

    /** Asignar responsable (ADMIN y GERENTE: lo exige AuthFilter). null deja sin asignar. */
    public ClienteDtos.Detalle asignar(long id, Integer idResponsable, UsuarioSesion actor) {
        return tx.enTransaccion(em -> {
            ClientePotencial c = buscar(em, id, actor);
            c.setResponsable(idResponsable == null ? null : usuarioActivo(em, idResponsable));
            return detalle(c);
        });
    }

    // ---- Apoyo ----

    private static ClientePotencial buscar(EntityManager em, long id, UsuarioSesion actor) {
        ClientePotencial c = em.find(ClientePotencial.class, id);
        Integer vendedor = alcance(actor);
        if (c == null || (vendedor != null && (c.getResponsable() == null || !vendedor.equals(c.getResponsable().getId())))) {
            throw new NoEncontradoException("El cliente potencial no existe");
        }
        return c;
    }

    private static Propiedad propiedad(EntityManager em, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        List<Propiedad> l = em.createQuery("select p from Propiedad p where p.codigo = :c", Propiedad.class)
                .setParameter("c", codigo.strip().toLowerCase(Locale.ROOT)).getResultList();
        if (l.isEmpty()) {
            throw ValidacionException.deCampo("codigoPropiedad", "La propiedad indicada no existe");
        }
        return l.get(0);
    }

    /** Responsable por defecto: el de la propiedad de interés, si existe y está activo. */
    static Usuario responsableDe(Propiedad p) {
        return p != null && p.getResponsable() != null && p.getResponsable().isActivo() ? p.getResponsable() : null;
    }

    private static Usuario usuarioActivo(EntityManager em, int id) {
        Usuario u = em.find(Usuario.class, id);
        if (u == null || !u.isActivo()) {
            throw ValidacionException.deCampo("idResponsable", "El responsable debe ser un usuario activo");
        }
        return u;
    }

    static String opcionEn(String valor, Set<String> validos, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String v = valor.strip().toUpperCase(Locale.ROOT);
        if (!validos.contains(v)) {
            throw ValidacionException.deCampo(campo, "Valor no válido");
        }
        return v;
    }

    private static ClienteDtos.Ref ref(Propiedad p) {
        return p == null ? null : new ClienteDtos.Ref(p.getId(), p.getCodigo(), p.getTitulo());
    }

    private static ClienteDtos.Persona persona(Usuario u) {
        return u == null ? null : new ClienteDtos.Persona(u.getId(), u.getNombre() + " " + u.getApellido(), u.isActivo());
    }

    static ClienteDtos.Resumen resumen(ClientePotencial c) {
        String contacto = c.getTelefono() != null ? c.getTelefono() : c.getEmail();
        String interes = c.getInteres() == null ? null : (c.getInteres().length() > 120 ? c.getInteres().substring(0, 119) + "…" : c.getInteres());
        return new ClienteDtos.Resumen(c.getId(), c.getNombre(), contacto, interes, ref(c.getPropiedad()), c.getOrigen(), c.getEstado(),
                persona(c.getResponsable()), c.getCreadoEn());
    }

    static ClienteDtos.Detalle detalle(ClientePotencial c) {
        return new ClienteDtos.Detalle(c.getId(), c.getNombre(), c.getTelefono(), c.getEmail(), c.getInteres(), ref(c.getPropiedad()),
                c.getConsulta() == null ? null : c.getConsulta().getId(), c.getOrigen(), c.getEstado(), persona(c.getResponsable()),
                persona(c.getRegistradoPor()), c.getConsentimientoEn(), c.getConsentimientoVersion(), c.getCreadoEn(), c.getActualizadoEn());
    }
}

package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Tabla {@code consulta}: preguntas al asistente de IA y sus respuestas. */
@Entity
@Table(name = "consulta")
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consulta")
    private Long id;

    /** Identificador de la cookie de sesión del visitante (no requiere cuenta). */
    @Column(name = "id_sesion", nullable = false, columnDefinition = "uuid")
    private UUID idSesion;

    @Column(name = "consulta", nullable = false, columnDefinition = "text")
    private String consulta;

    @Column(name = "respuesta", columnDefinition = "text")
    private String respuesta;

    /** Propiedad desde la que se consultó, si corresponde. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_propiedad")
    private Propiedad propiedad;

    /** Lo completa PostgreSQL (DEFAULT now()). */
    @Column(name = "fecha_hora", nullable = false, insertable = false, updatable = false,
            columnDefinition = "timestamptz")
    private OffsetDateTime fechaHora;

    protected Consulta() {
    }

    /** RESPONDIDA, DERIVADA_ASESOR, SIN_SERVICIO o BLOQUEADA; null en las consultas anteriores a la migración 003. */
    @Column(name = "resultado", length = 20)
    private String resultado;

    /** Revisión INTERNA del equipo (PENDIENTE o REVISADA); no indica si el cliente fue atendido. */
    @Column(name = "estado_revision", nullable = false, length = 10)
    private String estadoRevision = "PENDIENTE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisada_por")
    private Usuario revisadaPor;

    @Column(name = "revisada_en")
    private OffsetDateTime revisadaEn;

    /** Propiedades que el asistente recomendó en su respuesta (registradas desde la migración 003). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "consulta_recomendacion",
            joinColumns = @JoinColumn(name = "id_consulta"),
            inverseJoinColumns = @JoinColumn(name = "id_propiedad"))
    private Set<Propiedad> recomendadas = new LinkedHashSet<>();

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getEstadoRevision() { return estadoRevision; }
    public Usuario getRevisadaPor() { return revisadaPor; }
    public OffsetDateTime getRevisadaEn() { return revisadaEn; }
    public Set<Propiedad> getRecomendadas() { return recomendadas; }

    /** Marca la revisión interna; volver a PENDIENTE borra quién y cuándo. */
    public void revisar(boolean revisada, Usuario por, OffsetDateTime cuando) {
        this.estadoRevision = revisada ? "REVISADA" : "PENDIENTE";
        this.revisadaPor = revisada ? por : null;
        this.revisadaEn = revisada ? cuando : null;
    }

    public Consulta(UUID idSesion, String consulta, Propiedad propiedad) {
        this.idSesion = idSesion;
        this.consulta = consulta;
        this.propiedad = propiedad;
    }

    public Long getId() {
        return id;
    }

    public UUID getIdSesion() {
        return idSesion;
    }

    public String getConsulta() {
        return consulta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }
}

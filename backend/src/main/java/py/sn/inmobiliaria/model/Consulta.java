package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
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

package py.sn.inmobiliaria.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Persona interesada que dejó sus datos para ser contactada (migración 005). Datos personales mínimos. */
@Entity
@Table(name = "cliente_potencial")
public class ClientePotencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente_potencial")
    private Long id;
    @Column(nullable = false, length = 120) private String nombre;
    @Column(length = 30) private String telefono;
    @Column(length = 120) private String email;
    @Column(length = 1000) private String interes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_propiedad") private Propiedad propiedad;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_consulta") private Consulta consulta;
    /** CHAT, CONTACTO (formulario de la página Contacto), WHATSAPP, LLAMADA u OTRO */
    @Column(nullable = false, length = 10) private String origen;
    /** PENDIENTE, CONTACTADO o DESCARTADO */
    @Column(nullable = false, length = 12) private String estado = "PENDIENTE";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_responsable") private Usuario responsable;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registrado_por") private Usuario registradoPor;
    @Column(name = "consentimiento_en") private OffsetDateTime consentimientoEn;
    @Column(name = "consentimiento_version", length = 20) private String consentimientoVersion;
    @Column(name = "creado_en", nullable = false, updatable = false) private OffsetDateTime creadoEn = OffsetDateTime.now();
    @Column(name = "actualizado_en", nullable = false) private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    protected ClientePotencial() {
    }

    public ClientePotencial(String nombre, String telefono, String email, String interes, String origen) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.interes = interes;
        this.origen = origen;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getInteres() { return interes; }
    public Propiedad getPropiedad() { return propiedad; }
    public void setPropiedad(Propiedad propiedad) { this.propiedad = propiedad; }
    public Consulta getConsulta() { return consulta; }
    public void setConsulta(Consulta consulta) { this.consulta = consulta; }
    public String getOrigen() { return origen; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; this.actualizadoEn = OffsetDateTime.now(); }
    public Usuario getResponsable() { return responsable; }
    public void setResponsable(Usuario responsable) { this.responsable = responsable; this.actualizadoEn = OffsetDateTime.now(); }
    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }
    public OffsetDateTime getConsentimientoEn() { return consentimientoEn; }
    public String getConsentimientoVersion() { return consentimientoVersion; }
    public void registrarConsentimiento(OffsetDateTime en, String version) { this.consentimientoEn = en; this.consentimientoVersion = version; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
}

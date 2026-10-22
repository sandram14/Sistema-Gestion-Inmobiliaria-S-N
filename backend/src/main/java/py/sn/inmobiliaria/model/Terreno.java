package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Tabla {@code terreno}: datos específicos de una propiedad de tipo Terreno (relación 1 a 1). */
@Entity
@Table(name = "terreno")
public class Terreno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_terreno")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_propiedad", nullable = false, unique = true)
    private Propiedad propiedad;

    /** m² */
    @Column(name = "superficie", nullable = false, precision = 12, scale = 2)
    private BigDecimal superficie;

    /** m, opcional */
    @Column(name = "frente", precision = 8, scale = 2)
    private BigDecimal frente;

    /** m, opcional */
    @Column(name = "fondo", precision = 8, scale = 2)
    private BigDecimal fondo;

    @Column(name = "tiene_titulo", nullable = false)
    private boolean tieneTitulo;

    protected Terreno() {
    }

    public Terreno(BigDecimal superficie) {
        this.superficie = superficie;
    }

    public Long getId() {
        return id;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    /** Uso interno: la relación se establece con {@link Propiedad#asignarTerreno(Terreno)}. */
    void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
    }

    public BigDecimal getSuperficie() {
        return superficie;
    }

    public void setSuperficie(BigDecimal superficie) {
        this.superficie = superficie;
    }

    public BigDecimal getFrente() {
        return frente;
    }

    public void setFrente(BigDecimal frente) {
        this.frente = frente;
    }

    public BigDecimal getFondo() {
        return fondo;
    }

    public void setFondo(BigDecimal fondo) {
        this.fondo = fondo;
    }

    public boolean isTieneTitulo() {
        return tieneTitulo;
    }

    public void setTieneTitulo(boolean tieneTitulo) {
        this.tieneTitulo = tieneTitulo;
    }
}

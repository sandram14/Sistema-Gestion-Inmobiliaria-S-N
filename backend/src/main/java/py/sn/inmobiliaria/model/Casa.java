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

/** Tabla {@code casa}: datos específicos de una propiedad de tipo Casa (relación 1 a 1). */
@Entity
@Table(name = "casa")
public class Casa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_casa")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_propiedad", nullable = false, unique = true)
    private Propiedad propiedad;

    @Column(name = "habitaciones", nullable = false)
    private Short habitaciones;

    @Column(name = "banos", nullable = false)
    private Short banos;

    @Column(name = "sala", nullable = false)
    private boolean sala;

    @Column(name = "comedor", nullable = false)
    private boolean comedor;

    @Column(name = "cocina", nullable = false)
    private boolean cocina;

    /** m² */
    @Column(name = "superficie_construida", nullable = false, precision = 10, scale = 2)
    private BigDecimal superficieConstruida;

    /** m², opcional */
    @Column(name = "superficie_terreno", precision = 10, scale = 2)
    private BigDecimal superficieTerreno;

    protected Casa() {
    }

    public Casa(short habitaciones, short banos, BigDecimal superficieConstruida) {
        this.habitaciones = habitaciones;
        this.banos = banos;
        this.superficieConstruida = superficieConstruida;
    }

    public Long getId() {
        return id;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    /** Uso interno: la relación se establece con {@link Propiedad#asignarCasa(Casa)}. */
    void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
    }

    public Short getHabitaciones() {
        return habitaciones;
    }

    public void setHabitaciones(Short habitaciones) {
        this.habitaciones = habitaciones;
    }

    public Short getBanos() {
        return banos;
    }

    public void setBanos(Short banos) {
        this.banos = banos;
    }

    public boolean isSala() {
        return sala;
    }

    public void setSala(boolean sala) {
        this.sala = sala;
    }

    public boolean isComedor() {
        return comedor;
    }

    public void setComedor(boolean comedor) {
        this.comedor = comedor;
    }

    public boolean isCocina() {
        return cocina;
    }

    public void setCocina(boolean cocina) {
        this.cocina = cocina;
    }

    public BigDecimal getSuperficieConstruida() {
        return superficieConstruida;
    }

    public void setSuperficieConstruida(BigDecimal superficieConstruida) {
        this.superficieConstruida = superficieConstruida;
    }

    public BigDecimal getSuperficieTerreno() {
        return superficieTerreno;
    }

    public void setSuperficieTerreno(BigDecimal superficieTerreno) {
        this.superficieTerreno = superficieTerreno;
    }
}

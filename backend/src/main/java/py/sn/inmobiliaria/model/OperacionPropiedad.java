package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Tabla {@code operacion_propiedad}: qué operaciones ofrece una propiedad y a qué precio.
 * Ejemplo: Casa 001 → Venta USD 120.000 y Alquiler PYG 4.000.000.
 *
 * <p>{@code @MapsId} indica que la clave compuesta se toma de las dos relaciones,
 * así no hay que cargar los ids a mano.</p>
 */
@Entity
@Table(name = "operacion_propiedad")
public class OperacionPropiedad {

    @EmbeddedId
    private OperacionPropiedadId id = new OperacionPropiedadId();

    @MapsId("idPropiedad")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_propiedad", nullable = false)
    private Propiedad propiedad;

    @MapsId("idOperacion")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_operacion", nullable = false)
    private Operacion operacion;

    @Column(name = "precio", nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", nullable = false, length = 3, columnDefinition = "bpchar(3)")
    private Moneda moneda;

    protected OperacionPropiedad() {
    }

    public OperacionPropiedad(Operacion operacion, BigDecimal precio, Moneda moneda) {
        this.operacion = operacion;
        this.precio = precio;
        this.moneda = moneda;
    }

    public OperacionPropiedadId getId() {
        return id;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    /** Uso interno: la relación se establece con {@link Propiedad#agregarOperacion(OperacionPropiedad)}. */
    void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
    }

    public Operacion getOperacion() {
        return operacion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public void setMoneda(Moneda moneda) {
        this.moneda = moneda;
    }
}

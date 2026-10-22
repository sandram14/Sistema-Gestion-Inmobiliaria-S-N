package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave primaria compuesta de {@code operacion_propiedad}: (id_propiedad, id_operacion). */
@Embeddable
public class OperacionPropiedadId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_propiedad")
    private Long idPropiedad;

    @Column(name = "id_operacion")
    private Integer idOperacion;

    protected OperacionPropiedadId() {
    }

    public OperacionPropiedadId(Long idPropiedad, Integer idOperacion) {
        this.idPropiedad = idPropiedad;
        this.idOperacion = idOperacion;
    }

    public Long getIdPropiedad() {
        return idPropiedad;
    }

    public Integer getIdOperacion() {
        return idOperacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperacionPropiedadId otro)) {
            return false;
        }
        return Objects.equals(idPropiedad, otro.idPropiedad) && Objects.equals(idOperacion, otro.idOperacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPropiedad, idOperacion);
    }
}

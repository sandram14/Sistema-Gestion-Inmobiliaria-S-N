package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tabla {@code barrio}. El id es la CLAVE_BAR oficial (no autogenerada).
 * El polígono {@code geom} no se mapea: se lee con SQL nativo PostGIS.
 */
@Entity
@Table(name = "barrio")
public class Barrio {

    @Id
    @Column(name = "id_barrio")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ciudad", nullable = false)
    private Ciudad ciudad;

    protected Barrio() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }
}

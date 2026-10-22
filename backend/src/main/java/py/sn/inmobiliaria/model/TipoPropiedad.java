package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla {@code tipo_propiedad} (catálogo). Tipo de propiedad: Casa o Terreno. */
@Entity
@Table(name = "tipo_propiedad")
public class TipoPropiedad {

    /** Valores cargados en database/seed/01_catalogos.sql. */
    public static final String CASA = "Casa";
    public static final String TERRENO = "Terreno";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_propiedad")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    protected TipoPropiedad() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

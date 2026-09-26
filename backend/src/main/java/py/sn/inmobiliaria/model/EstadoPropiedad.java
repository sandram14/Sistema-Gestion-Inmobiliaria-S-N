package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla {@code estado_propiedad} (catálogo). Estado: Disponible, Reservado, Vendido, Alquilado o Inactivo. */
@Entity
@Table(name = "estado_propiedad")
public class EstadoPropiedad {

    /** Valores cargados en database/seed/01_catalogos.sql. */
    public static final String DISPONIBLE = "Disponible";
    public static final String RESERVADO = "Reservado";
    public static final String VENDIDO = "Vendido";
    public static final String ALQUILADO = "Alquilado";
    public static final String INACTIVO = "Inactivo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    protected EstadoPropiedad() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

package py.sn.inmobiliaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla {@code operacion} (catálogo). Operación: Venta o Alquiler. */
@Entity
@Table(name = "operacion")
public class Operacion {

    /** Valores cargados en database/seed/01_catalogos.sql. */
    public static final String VENTA = "Venta";
    public static final String ALQUILER = "Alquiler";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operacion")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    protected Operacion() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

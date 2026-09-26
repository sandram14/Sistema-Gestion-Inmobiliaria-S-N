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

/**
 * Tabla {@code imagen}. Una propiedad puede tener varias imágenes y como máximo
 * una principal (índice único parcial uq_imagen_principal).
 */
@Entity
@Table(name = "imagen")
public class Imagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_propiedad", nullable = false)
    private Propiedad propiedad;

    /** Ruta relativa (/images/..., /uploads/...) o URL https. */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    /** Posición en el carrusel. */
    @Column(name = "orden", nullable = false)
    private Short orden;

    protected Imagen() {
    }

    public Imagen(String url, boolean esPrincipal, short orden) {
        this.url = url;
        this.esPrincipal = esPrincipal;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public Propiedad getPropiedad() {
        return propiedad;
    }

    /** Uso interno: la relación se establece con {@link Propiedad#agregarImagen(Imagen)}. */
    void setPropiedad(Propiedad propiedad) {
        this.propiedad = propiedad;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }

    public void setEsPrincipal(boolean esPrincipal) {
        this.esPrincipal = esPrincipal;
    }

    public Short getOrden() {
        return orden;
    }

    public void setOrden(Short orden) {
        this.orden = orden;
    }
}

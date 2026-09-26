package py.sn.inmobiliaria.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tabla {@code propiedad}: datos comunes a casas y terrenos.
 *
 * <p>Los datos específicos están en {@link Casa} o {@link Terreno} (uno de los dos,
 * según el tipo). La columna {@code ubicacion} la calcula PostgreSQL a partir de
 * latitud/longitud, por eso no se mapea aquí.</p>
 */
@Entity
@Table(name = "propiedad")
public class Propiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propiedad")
    private Long id;

    /** Código público (prop-001, terr-001): el que usa el frontend en URLs y carpetas de imágenes. */
    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "latitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_propiedad", nullable = false)
    private TipoPropiedad tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoPropiedad estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_barrio", nullable = false)
    private Barrio barrio;

    /** Lo completa PostgreSQL (DEFAULT now()). */
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false,
            columnDefinition = "timestamptz")
    private OffsetDateTime creadoEn;

    /** Lo mantiene el trigger trg_propiedad_actualizado_en. */
    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false,
            columnDefinition = "timestamptz")
    private OffsetDateTime actualizadoEn;

    @OneToOne(mappedBy = "propiedad", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Casa casa;

    @OneToOne(mappedBy = "propiedad", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Terreno terreno;

    /** Orden del carrusel: primero la principal, luego por el campo orden. */
    @OneToMany(mappedBy = "propiedad", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("esPrincipal DESC, orden ASC")
    private List<Imagen> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "propiedad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperacionPropiedad> operaciones = new ArrayList<>();

    protected Propiedad() {
    }

    public Propiedad(String codigo, String titulo, BigDecimal latitud, BigDecimal longitud,
                     TipoPropiedad tipo, EstadoPropiedad estado, Barrio barrio) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipo = tipo;
        this.estado = estado;
        this.barrio = barrio;
    }

    // ---- Métodos que mantienen sincronizados ambos lados de las relaciones ----

    /** Asigna los datos de casa (reemplaza los anteriores, si los había). */
    public void asignarCasa(Casa nuevaCasa) {
        this.casa = nuevaCasa;
        if (nuevaCasa != null) {
            nuevaCasa.setPropiedad(this);
        }
    }

    /** Asigna los datos de terreno (reemplaza los anteriores, si los había). */
    public void asignarTerreno(Terreno nuevoTerreno) {
        this.terreno = nuevoTerreno;
        if (nuevoTerreno != null) {
            nuevoTerreno.setPropiedad(this);
        }
    }

    public void agregarImagen(Imagen imagen) {
        imagen.setPropiedad(this);
        imagenes.add(imagen);
    }

    public void quitarImagen(Imagen imagen) {
        imagenes.remove(imagen);
        imagen.setPropiedad(null);
    }

    public void agregarOperacion(OperacionPropiedad operacion) {
        operacion.setPropiedad(this);
        operaciones.add(operacion);
    }

    public void quitarOperacion(OperacionPropiedad operacion) {
        operaciones.remove(operacion);
        operacion.setPropiedad(null);
    }

    // ---- Getters y setters ----

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public TipoPropiedad getTipo() {
        return tipo;
    }

    public void setTipo(TipoPropiedad tipo) {
        this.tipo = tipo;
    }

    public EstadoPropiedad getEstado() {
        return estado;
    }

    public void setEstado(EstadoPropiedad estado) {
        this.estado = estado;
    }

    public Barrio getBarrio() {
        return barrio;
    }

    public void setBarrio(Barrio barrio) {
        this.barrio = barrio;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public Casa getCasa() {
        return casa;
    }

    public Terreno getTerreno() {
        return terreno;
    }

    /** Lista de solo lectura: para modificarla usar agregarImagen / quitarImagen. */
    public List<Imagen> getImagenes() {
        return Collections.unmodifiableList(imagenes);
    }

    /** Lista de solo lectura: para modificarla usar agregarOperacion / quitarOperacion. */
    public List<OperacionPropiedad> getOperaciones() {
        return Collections.unmodifiableList(operaciones);
    }
}

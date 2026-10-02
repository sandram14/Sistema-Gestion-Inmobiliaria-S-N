package py.sn.inmobiliaria.service;

import jakarta.persistence.EntityManager;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.request.ImagenRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;
import py.sn.inmobiliaria.exception.ValidacionException;
import py.sn.inmobiliaria.model.Barrio;
import py.sn.inmobiliaria.model.Casa;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.OperacionPropiedad;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Terreno;
import py.sn.inmobiliaria.model.TipoPropiedad;
import py.sn.inmobiliaria.repository.CatalogoDao;
import py.sn.inmobiliaria.repository.ImagenDao;
import py.sn.inmobiliaria.repository.PropiedadDao;
import py.sn.inmobiliaria.repository.UbicacionDao;
import py.sn.inmobiliaria.service.validacion.Errores;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada.CasaValidada;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada.OperacionValidada;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada.TerrenoValidado;
import py.sn.inmobiliaria.service.validacion.ValidadorPropiedad;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestión de propiedades desde el panel administrativo.
 *
 * <p>Cada operación de escritura es una única transacción: crear una propiedad con su casa,
 * sus operaciones y sus imágenes se guarda completa o no se guarda.</p>
 */
public class PropiedadAdminService extends ServicioBase {

    private final PropiedadDao propiedadDao;
    private final CatalogoDao catalogoDao;
    private final ImagenDao imagenDao;
    private final UbicacionDao ubicacionDao;

    public PropiedadAdminService(TxRunner tx, PropiedadDao propiedadDao, CatalogoDao catalogoDao,
                                 ImagenDao imagenDao, UbicacionDao ubicacionDao) {
        super(tx);
        this.propiedadDao = propiedadDao;
        this.catalogoDao = catalogoDao;
        this.imagenDao = imagenDao;
        this.ubicacionDao = ubicacionDao;
    }

    /** Resultado de crear o actualizar: el id interno y el código público. */
    public record PropiedadGuardada(long id, String codigo) {
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    /** Todas las propiedades, en cualquier estado. */
    public <T> List<T> listar(Function<Propiedad, T> conversor) {
        return leer(em -> propiedadDao.listarTodas(em).stream().map(conversor).toList());
    }

    public <T> T obtener(long id, Function<Propiedad, T> conversor) {
        return leer(em -> conversor.apply(buscarOFallar(em, id)));
    }

    // =====================================================================
    // Crear / actualizar / eliminar
    // =====================================================================

    public PropiedadGuardada crear(PropiedadRequest req) {
        PropiedadValidada v = ValidadorPropiedad.validar(req);
        verificarUbicacion(v);

        return escribir(em -> {
            TipoPropiedad tipo = catalogo(catalogoDao.tipoPorNombre(em, v.tipo()), "tipo");
            String nombreEstado = v.estado() != null ? v.estado() : EstadoPropiedad.DISPONIBLE;
            EstadoPropiedad estado = catalogo(catalogoDao.estadoPorNombre(em, nombreEstado), "estado");
            Barrio barrio = barrio(em, v.idBarrio());

            String prefijo = ReglasNegocio.prefijoCodigo(v.tipo());
            String codigo = ReglasNegocio.formatearCodigo(prefijo, propiedadDao.ultimoNumeroDeCodigo(em, prefijo) + 1);

            Propiedad p = new Propiedad(codigo, v.titulo(), v.latitud(), v.longitud(), tipo, estado, barrio);
            p.setDescripcion(v.descripcion());
            p.setDireccion(v.direccion());
            if (v.casa() != null) {
                p.asignarCasa(nuevaCasa(v.casa()));
            } else {
                p.asignarTerreno(nuevoTerreno(v.terreno()));
            }
            for (OperacionValidada op : v.operaciones()) {
                p.agregarOperacion(nuevaOperacion(em, op));
            }

            propiedadDao.guardar(em, p);
            em.flush(); // obtiene el id y ejecuta los triggers dentro de la transacción
            return new PropiedadGuardada(p.getId(), p.getCodigo());
        });
    }

    /**
     * Reemplaza los datos de la propiedad por los recibidos. El código público no cambia
     * (aunque cambie el tipo), para no romper enlaces ni carpetas de imágenes.
     */
    public PropiedadGuardada actualizar(long id, PropiedadRequest req) {
        PropiedadValidada v = ValidadorPropiedad.validar(req);
        verificarUbicacion(v);

        return escribir(em -> {
            Propiedad p = buscarOFallar(em, id);
            Map<String, OperacionValidada> nuevas = v.operaciones().stream()
                    .collect(Collectors.toMap(OperacionValidada::operacion, Function.identity()));

            // 1) Quitar operaciones que ya no están (por ejemplo, Alquiler al pasar de Casa a Terreno)
            for (OperacionPropiedad actual : new ArrayList<>(p.getOperaciones())) {
                if (!nuevas.containsKey(actual.getOperacion().getNombre())) {
                    p.quitarOperacion(actual);
                }
            }
            em.flush();

            // 2) Cambio de tipo: primero se eliminan los datos del subtipo anterior
            //    (los triggers no permiten una propiedad con datos de casa y terreno a la vez)
            if (!p.getTipo().getNombre().equals(v.tipo())) {
                if (p.getCasa() != null) {
                    em.remove(p.getCasa());
                    p.asignarCasa(null);
                }
                if (p.getTerreno() != null) {
                    em.remove(p.getTerreno());
                    p.asignarTerreno(null);
                }
                em.flush();
                p.setTipo(catalogo(catalogoDao.tipoPorNombre(em, v.tipo()), "tipo"));
                em.flush();
            }

            // 3) Datos generales
            p.setTitulo(v.titulo());
            p.setDescripcion(v.descripcion());
            p.setDireccion(v.direccion());
            p.setLatitud(v.latitud());
            p.setLongitud(v.longitud());
            if (p.getBarrio().getId() != v.idBarrio()) {
                p.setBarrio(barrio(em, v.idBarrio()));
            }
            if (v.estado() != null && !p.getEstado().getNombre().equals(v.estado())) {
                p.setEstado(catalogo(catalogoDao.estadoPorNombre(em, v.estado()), "estado"));
            }

            // 4) Datos del subtipo
            if (v.casa() != null) {
                if (p.getCasa() == null) {
                    p.asignarCasa(nuevaCasa(v.casa()));
                } else {
                    copiarCasa(v.casa(), p.getCasa());
                }
            } else if (p.getTerreno() == null) {
                p.asignarTerreno(nuevoTerreno(v.terreno()));
            } else {
                copiarTerreno(v.terreno(), p.getTerreno());
            }

            // 5) Agregar o actualizar operaciones
            for (OperacionValidada op : v.operaciones()) {
                Optional<OperacionPropiedad> existente = p.getOperaciones().stream()
                        .filter(o -> o.getOperacion().getNombre().equals(op.operacion()))
                        .findFirst();
                if (existente.isPresent()) {
                    existente.get().setPrecio(op.precio());
                    existente.get().setMoneda(op.moneda());
                } else {
                    p.agregarOperacion(nuevaOperacion(em, op));
                }
            }

            em.flush();
            return new PropiedadGuardada(p.getId(), p.getCodigo());
        });
    }

    /** Borrado lógico (decisión aprobada): la propiedad pasa a Inactivo y deja de verse en la web. */
    public void eliminar(long id) {
        escribir(em -> {
            Propiedad p = buscarOFallar(em, id);
            p.setEstado(catalogo(catalogoDao.estadoPorNombre(em, EstadoPropiedad.INACTIVO), "estado"));
            return null;
        });
    }

    // =====================================================================
    // Imágenes (el archivo en sí se guarda en la Fase 11; aquí se registra en la base)
    // =====================================================================

    /** Registra una imagen; si es la primera de la propiedad, queda como principal. */
    public long agregarImagen(long idPropiedad, ImagenRequest req) {
        Errores e = new Errores();
        String url = req == null ? null : texto(req.url());
        if (url == null) {
            e.agregar("url", "La URL de la imagen es obligatoria");
        } else if (url.length() > 500) {
            e.agregar("url", "Máximo 500 caracteres");
        } else if (!(url.startsWith("/") || url.startsWith("https://"))) {
            e.agregar("url", "Debe ser una ruta relativa (/...) o una URL https");
        }
        String descripcion = req == null ? null : texto(req.descripcion());
        if (descripcion != null && descripcion.length() > 200) {
            e.agregar("descripcion", "Máximo 200 caracteres");
        }
        e.lanzarSiHay();

        return escribir(em -> {
            Propiedad p = buscarOFallar(em, idPropiedad);
            short orden = (short) (p.getImagenes().stream()
                    .map(Imagen::getOrden).max(Comparator.naturalOrder()).orElse((short) -1) + 1);
            Imagen imagen = new Imagen(url, p.getImagenes().isEmpty(), orden);
            imagen.setDescripcion(descripcion);
            p.agregarImagen(imagen);
            em.flush();
            return imagen.getId();
        });
    }

    /** Elimina una imagen; si era la principal, la siguiente en orden pasa a serlo. */
    public void eliminarImagen(long idPropiedad, long idImagen) {
        escribir(em -> {
            Imagen imagen = imagenDao.buscar(em, idPropiedad, idImagen)
                    .orElseThrow(() -> new NoEncontradoException("La imagen no existe en esta propiedad"));
            Propiedad p = imagen.getPropiedad();
            boolean eraPrincipal = imagen.isEsPrincipal();
            p.quitarImagen(imagen);
            em.flush();
            if (eraPrincipal) {
                p.getImagenes().stream()
                        .min(Comparator.comparing(Imagen::getOrden))
                        .ifPresent(siguiente -> siguiente.setEsPrincipal(true));
            }
            return null;
        });
    }

    public void marcarPrincipal(long idPropiedad, long idImagen) {
        escribir(em -> {
            Imagen imagen = imagenDao.buscar(em, idPropiedad, idImagen)
                    .orElseThrow(() -> new NoEncontradoException("La imagen no existe en esta propiedad"));
            if (!imagen.isEsPrincipal()) {
                imagenDao.desmarcarPrincipal(em, idPropiedad); // primero se quita la anterior
                imagen.setEsPrincipal(true);
            }
            return null;
        });
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    /** Verifica con PostGIS que el punto marcado esté dentro del barrio elegido. */
    private void verificarUbicacion(PropiedadValidada v) {
        Optional<Boolean> dentro;
        try {
            dentro = ubicacionDao.puntoDentroDeBarrio(v.idBarrio(), v.latitud(), v.longitud());
        } catch (SQLException e) {
            throw new ServicioNoDisponibleException("No se pudo verificar la ubicación. Intente nuevamente.", e);
        }
        if (dentro.isEmpty()) {
            throw ValidacionException.deCampo("idBarrio", "El barrio seleccionado no existe");
        }
        if (!dentro.get()) {
            throw ValidacionException.deCampo("latitud",
                    "La ubicación marcada en el mapa no está dentro del barrio seleccionado");
        }
    }

    private Propiedad buscarOFallar(EntityManager em, long id) {
        return propiedadDao.buscarPorId(em, id)
                .orElseThrow(() -> new NoEncontradoException("La propiedad no existe"));
    }

    private Barrio barrio(EntityManager em, int idBarrio) {
        return catalogoDao.barrioPorId(em, idBarrio)
                .orElseThrow(() -> ValidacionException.deCampo("idBarrio", "El barrio seleccionado no existe"));
    }

    /** Un catálogo validado siempre existe; si no, la base no tiene la carga inicial. */
    private static <T> T catalogo(Optional<T> valor, String campo) {
        return valor.orElseThrow(() -> new IllegalStateException(
                "Catálogo incompleto en la base de datos: " + campo + " (revise database/seed/01_catalogos.sql)"));
    }

    private OperacionPropiedad nuevaOperacion(EntityManager em, OperacionValidada op) {
        Operacion operacion = catalogo(catalogoDao.operacionPorNombre(em, op.operacion()), "operacion");
        return new OperacionPropiedad(operacion, op.precio(), op.moneda());
    }

    private static Casa nuevaCasa(CasaValidada c) {
        Casa casa = new Casa(c.habitaciones(), c.banos(), c.superficieConstruida());
        copiarCasa(c, casa);
        return casa;
    }

    private static void copiarCasa(CasaValidada c, Casa casa) {
        casa.setHabitaciones(c.habitaciones());
        casa.setBanos(c.banos());
        casa.setSala(c.sala());
        casa.setComedor(c.comedor());
        casa.setCocina(c.cocina());
        casa.setSuperficieConstruida(c.superficieConstruida());
        casa.setSuperficieTerreno(c.superficieTerreno());
    }

    private static Terreno nuevoTerreno(TerrenoValidado t) {
        Terreno terreno = new Terreno(t.superficie());
        copiarTerreno(t, terreno);
        return terreno;
    }

    private static void copiarTerreno(TerrenoValidado t, Terreno terreno) {
        terreno.setSuperficie(t.superficie());
        terreno.setFrente(t.frente());
        terreno.setFondo(t.fondo());
        terreno.setTieneTitulo(t.tieneTitulo());
    }

    private static String texto(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }
}

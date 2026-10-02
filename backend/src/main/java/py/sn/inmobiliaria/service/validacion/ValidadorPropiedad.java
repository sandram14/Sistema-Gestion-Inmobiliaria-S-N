package py.sn.inmobiliaria.service.validacion;

import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.CasaRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.OperacionRequest;
import py.sn.inmobiliaria.dto.request.PropiedadRequest.TerrenoRequest;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.TipoPropiedad;
import py.sn.inmobiliaria.service.ReglasNegocio;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada.CasaValidada;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada.OperacionValidada;
import py.sn.inmobiliaria.service.validacion.PropiedadValidada.TerrenoValidado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Valida los datos de una propiedad SIN tocar la base de datos (formato, rangos y reglas
 * de negocio). Las verificaciones que necesitan la base (barrio existente, punto dentro
 * del barrio) las hace PropiedadAdminService.
 *
 * <p>Los límites numéricos coinciden con las columnas de PostgreSQL
 * (por ejemplo, numeric(10,2) admite hasta 99.999.999,99).</p>
 */
public final class ValidadorPropiedad {

    static final int MAX_TITULO = 150;
    static final int MAX_DESCRIPCION = 5000;
    static final int MAX_DIRECCION = 200;
    static final int MAX_AMBIENTES = 100;

    private static final BigDecimal MAX_SUP_CASA = new BigDecimal("99999999.99");        // numeric(10,2)
    private static final BigDecimal MAX_SUP_TERRENO = new BigDecimal("9999999999.99");   // numeric(12,2)
    private static final BigDecimal MAX_MEDIDA = new BigDecimal("999999.99");            // numeric(8,2)
    private static final BigDecimal MAX_PRECIO = new BigDecimal("9999999999999.99");     // numeric(15,2)
    private static final BigDecimal LAT_MAX = new BigDecimal("90");
    private static final BigDecimal LNG_MAX = new BigDecimal("180");

    private ValidadorPropiedad() {
    }

    /**
     * @return los datos normalizados
     * @throws py.sn.inmobiliaria.exception.ValidacionException con todos los errores encontrados
     */
    public static PropiedadValidada validar(PropiedadRequest req) {
        Errores e = new Errores();
        if (req == null) {
            e.agregar("propiedad", "No se recibieron datos");
            e.lanzarSiHay();
        }

        String titulo = texto(req.titulo());
        if (titulo == null) {
            e.agregar("titulo", "El título es obligatorio");
        } else if (titulo.length() > MAX_TITULO) {
            e.agregar("titulo", "Máximo " + MAX_TITULO + " caracteres");
        }
        String descripcion = texto(req.descripcion());
        if (descripcion != null && descripcion.length() > MAX_DESCRIPCION) {
            e.agregar("descripcion", "Máximo " + MAX_DESCRIPCION + " caracteres");
        }
        String direccion = texto(req.direccion());
        if (direccion != null && direccion.length() > MAX_DIRECCION) {
            e.agregar("direccion", "Máximo " + MAX_DIRECCION + " caracteres");
        }

        BigDecimal lat = coordenada(req.latitud(), LAT_MAX, "latitud", e);
        BigDecimal lng = coordenada(req.longitud(), LNG_MAX, "longitud", e);

        if (req.idBarrio() == null) {
            e.agregar("idBarrio", "Seleccione el barrio");
        } else if (req.idBarrio() <= 0) {
            e.agregar("idBarrio", "Barrio no válido");
        }

        String tipo = null;
        if (texto(req.tipo()) == null) {
            e.agregar("tipo", "Seleccione el tipo de propiedad");
        } else {
            tipo = ReglasNegocio.normalizarTipo(req.tipo())
                    .filter(t -> !"vivienda".equalsIgnoreCase(req.tipo().trim())) // en el panel se usa el nombre oficial
                    .orElse(null);
            if (tipo == null) {
                e.agregar("tipo", "Tipo no válido. Use Casa o Terreno");
            }
        }

        String estado = null;
        if (texto(req.estado()) != null) {
            estado = ReglasNegocio.normalizarEstado(req.estado()).orElse(null);
            if (estado == null) {
                e.agregar("estado", "Estado no válido. Use Disponible, Reservado, Vendido, Alquilado o Inactivo");
            }
        }

        CasaValidada casa = null;
        TerrenoValidado terreno = null;
        if (TipoPropiedad.CASA.equals(tipo)) {
            if (req.terreno() != null) {
                e.agregar("terreno", "Una casa no debe incluir datos de terreno");
            }
            casa = validarCasa(req.casa(), e);
        } else if (TipoPropiedad.TERRENO.equals(tipo)) {
            if (req.casa() != null) {
                e.agregar("casa", "Un terreno no debe incluir datos de casa");
            }
            terreno = validarTerreno(req.terreno(), e);
        }

        List<OperacionValidada> operaciones = validarOperaciones(req.operaciones(), tipo, e);

        e.lanzarSiHay();
        return new PropiedadValidada(titulo, descripcion, direccion, lat, lng, req.idBarrio(), tipo, estado,
                casa, terreno, operaciones);
    }

    private static CasaValidada validarCasa(CasaRequest c, Errores e) {
        if (c == null) {
            e.agregar("casa", "Complete los datos de la casa");
            return null;
        }
        Short habitaciones = cantidad(c.habitaciones(), "casa.habitaciones", e);
        Short banos = cantidad(c.banos(), "casa.banos", e);
        BigDecimal supConstruida = positivo(c.superficieConstruida(), MAX_SUP_CASA, true, "casa.superficieConstruida", e);
        BigDecimal supTerreno = positivo(c.superficieTerreno(), MAX_SUP_CASA, false, "casa.superficieTerreno", e);
        if (habitaciones == null || banos == null || supConstruida == null) {
            return null;
        }
        return new CasaValidada(habitaciones, banos, verdadero(c.sala()), verdadero(c.comedor()), verdadero(c.cocina()),
                supConstruida, supTerreno);
    }

    private static TerrenoValidado validarTerreno(TerrenoRequest t, Errores e) {
        if (t == null) {
            e.agregar("terreno", "Complete los datos del terreno");
            return null;
        }
        BigDecimal superficie = positivo(t.superficie(), MAX_SUP_TERRENO, true, "terreno.superficie", e);
        BigDecimal frente = positivo(t.frente(), MAX_MEDIDA, false, "terreno.frente", e);
        BigDecimal fondo = positivo(t.fondo(), MAX_MEDIDA, false, "terreno.fondo", e);
        if (superficie == null) {
            return null;
        }
        return new TerrenoValidado(superficie, frente, fondo, verdadero(t.tieneTitulo()));
    }

    private static List<OperacionValidada> validarOperaciones(List<OperacionRequest> ops, String tipo, Errores e) {
        List<OperacionValidada> resultado = new ArrayList<>();
        if (ops == null || ops.isEmpty()) {
            e.agregar("operaciones", "Indique al menos una operación (Venta o Alquiler) con su precio");
            return resultado;
        }
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < ops.size(); i++) {
            OperacionRequest op = ops.get(i);
            String base = "operaciones[" + i + "]";
            if (op == null) {
                e.agregar(base, "Operación vacía");
                continue;
            }
            String nombre = ReglasNegocio.normalizarOperacion(op.operacion()).orElse(null);
            if (nombre == null) {
                e.agregar(base + ".operacion", "Operación no válida. Use Venta o Alquiler");
            } else if (!vistas.add(nombre)) {
                e.agregar(base + ".operacion", "La operación " + nombre + " está repetida");
            } else if (tipo != null && !ReglasNegocio.operacionPermitida(tipo, nombre)) {
                e.agregar(base + ".operacion", "Un terreno solamente puede tener la operación Venta");
            }
            BigDecimal precio = positivo(op.precio(), MAX_PRECIO, true, base + ".precio", e);
            Moneda moneda = ReglasNegocio.normalizarMoneda(op.moneda()).orElse(null);
            if (moneda == null) {
                e.agregar(base + ".moneda", "Moneda no válida. Use PYG o USD");
            }
            if (nombre != null && precio != null && moneda != null) {
                resultado.add(new OperacionValidada(nombre, precio, moneda));
            }
        }
        return resultado;
    }

    // ---- Utilidades ----

    /** Recorta espacios; devuelve null si queda vacío. */
    static String texto(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }

    private static BigDecimal coordenada(BigDecimal valor, BigDecimal limite, String campo, Errores e) {
        if (valor == null) {
            e.agregar(campo, "Marque la ubicación en el mapa");
            return null;
        }
        if (valor.abs().compareTo(limite) > 0) {
            e.agregar(campo, "Fuera de rango (±" + limite + ")");
            return null;
        }
        return valor.setScale(6, RoundingMode.HALF_UP); // precisión de la columna numeric(9,6)
    }

    private static Short cantidad(Integer valor, String campo, Errores e) {
        if (valor == null) {
            e.agregar(campo, "Obligatorio");
            return null;
        }
        if (valor < 0 || valor > MAX_AMBIENTES) {
            e.agregar(campo, "Debe estar entre 0 y " + MAX_AMBIENTES);
            return null;
        }
        return valor.shortValue();
    }

    private static BigDecimal positivo(BigDecimal valor, BigDecimal maximo, boolean obligatorio, String campo, Errores e) {
        if (valor == null) {
            if (obligatorio) {
                e.agregar(campo, "Obligatorio");
            }
            return null;
        }
        BigDecimal redondeado = valor.setScale(2, RoundingMode.HALF_UP);
        if (redondeado.signum() <= 0) {
            e.agregar(campo, "Debe ser mayor a 0");
            return null;
        }
        if (redondeado.compareTo(maximo) > 0) {
            e.agregar(campo, "Valor demasiado grande");
            return null;
        }
        return redondeado;
    }

    private static boolean verdadero(Boolean b) {
        return Boolean.TRUE.equals(b);
    }
}

package py.sn.inmobiliaria.service.validacion;

import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.repository.FiltroPropiedades;
import py.sn.inmobiliaria.service.ReglasNegocio;

import java.math.BigDecimal;

/**
 * Convierte los criterios del frontend en un filtro válido para el DAO.
 *
 * <p>Acepta mayúsculas/minúsculas indistintas y el término "Vivienda" del frontend.
 * Rechaza combinaciones inválidas aunque el frontend ya las evite (por ejemplo,
 * Terreno + Alquiler).</p>
 */
public final class NormalizadorBusqueda {

    private NormalizadorBusqueda() {
    }

    public static FiltroPropiedades normalizar(BusquedaRequest req) {
        if (req == null) {
            return FiltroPropiedades.sinFiltros();
        }
        Errores errores = new Errores();

        if (req.departamentoId() != null && req.departamentoId() <= 0) {
            errores.agregar("departamentoId", "Debe ser un número positivo");
        }
        if (req.ciudadId() != null && req.ciudadId() <= 0) {
            errores.agregar("ciudadId", "Debe ser un número positivo");
        }
        if (req.barrioId() != null && req.barrioId() <= 0) {
            errores.agregar("barrioId", "Debe ser un número positivo");
        }

        String tipo = null;
        if (!vacio(req.categoria())) {
            tipo = ReglasNegocio.normalizarTipo(req.categoria()).orElse(null);
            if (tipo == null) {
                errores.agregar("categoria", "Valor no válido. Use Casa (o Vivienda) o Terreno");
            }
        }

        String operacion = null;
        if (!vacio(req.operacion())) {
            operacion = ReglasNegocio.normalizarOperacion(req.operacion()).orElse(null);
            if (operacion == null) {
                errores.agregar("operacion", "Valor no válido. Use Venta o Alquiler");
            }
        }

        if (tipo != null && operacion != null && !ReglasNegocio.operacionPermitida(tipo, operacion)) {
            errores.agregar("operacion", "Un terreno solamente puede buscarse en Venta");
        }

        BigDecimal presupuesto = req.presupuestoMax();
        BigDecimal minimo = req.presupuestoMin();
        Moneda moneda = null;
        if (minimo != null && minimo.signum() <= 0) {
            errores.agregar("presupuestoMin", "Debe ser mayor a 0");
        }
        if (minimo != null && presupuesto != null && minimo.compareTo(presupuesto) > 0) {
            errores.agregar("presupuestoMin", "No puede ser mayor que el presupuesto máximo");
        }
        if (presupuesto != null || minimo != null) {
            if (presupuesto != null && presupuesto.signum() <= 0) {
                errores.agregar("presupuestoMax", "Debe ser mayor a 0");
            }
            if (vacio(req.moneda())) {
                moneda = Moneda.USD; // decisión aprobada: USD por defecto
            } else {
                moneda = ReglasNegocio.normalizarMoneda(req.moneda()).orElse(null);
                if (moneda == null) {
                    errores.agregar("moneda", "Valor no válido. Use USD o PYG");
                }
            }
        }

        errores.lanzarSiHay();
        return new FiltroPropiedades(req.ciudadId(), req.barrioId(), tipo, operacion, presupuesto, moneda,
                req.departamentoId(), minimo);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}

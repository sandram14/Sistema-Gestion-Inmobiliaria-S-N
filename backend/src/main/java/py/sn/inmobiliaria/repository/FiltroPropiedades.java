package py.sn.inmobiliaria.repository;

import py.sn.inmobiliaria.model.Moneda;

import java.math.BigDecimal;

/**
 * Criterios de la búsqueda pública. Cada campo es opcional (null = sin filtrar).
 *
 * @param ciudadId       id de la ciudad
 * @param barrioId       id del barrio (CLAVE_BAR)
 * @param tipo           nombre del tipo: "Casa" o "Terreno"
 * @param operacion      nombre de la operación: "Venta" o "Alquiler"
 * @param presupuestoMax precio máximo, en guaraníes
 * @param moneda         moneda del presupuesto (siempre PYG); obligatoria si hay presupuesto
 * @param departamentoId id del departamento (por ejemplo, Alto Paraná)
 */
public record FiltroPropiedades(
        Integer ciudadId,
        Integer barrioId,
        String tipo,
        String operacion,
        BigDecimal presupuestoMax,
        Moneda moneda,
        Integer departamentoId,
        BigDecimal presupuestoMin) {

    /** Sin presupuesto mínimo (forma anterior). */
    public FiltroPropiedades(Integer ciudadId, Integer barrioId, String tipo, String operacion,
                             BigDecimal presupuestoMax, Moneda moneda, Integer departamentoId) {
        this(ciudadId, barrioId, tipo, operacion, presupuestoMax, moneda, departamentoId, null);
    }

    /** Sin filtro de departamento (constructor anterior a la Fase 12). */
    public FiltroPropiedades(Integer ciudadId, Integer barrioId, String tipo, String operacion,
                             BigDecimal presupuestoMax, Moneda moneda) {
        this(ciudadId, barrioId, tipo, operacion, presupuestoMax, moneda, null);
    }

    /** Filtro vacío: todas las propiedades disponibles. */
    public static FiltroPropiedades sinFiltros() {
        return new FiltroPropiedades(null, null, null, null, null, null, null, null);
    }

    public boolean tienePresupuesto() {
        return presupuestoMax != null || presupuestoMin != null;
    }
}

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
 * @param presupuestoMax precio máximo (se compara solo contra precios en {@code moneda})
 * @param moneda         moneda del presupuesto; obligatoria si hay presupuestoMax
 */
public record FiltroPropiedades(
        Integer ciudadId,
        Integer barrioId,
        String tipo,
        String operacion,
        BigDecimal presupuestoMax,
        Moneda moneda) {

    /** Filtro vacío: todas las propiedades disponibles. */
    public static FiltroPropiedades sinFiltros() {
        return new FiltroPropiedades(null, null, null, null, null, null);
    }

    public boolean tienePresupuesto() {
        return presupuestoMax != null;
    }
}

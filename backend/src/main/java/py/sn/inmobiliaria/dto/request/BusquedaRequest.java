package py.sn.inmobiliaria.dto.request;

import java.math.BigDecimal;

/**
 * Criterios de la búsqueda pública, tal como llegan del frontend. Todos opcionales.
 *
 * @param ciudadId       id de la ciudad
 * @param barrioId       id del barrio (CLAVE_BAR)
 * @param categoria      "Casa", "Terreno" o "Vivienda" (alias de Casa usado por el frontend)
 * @param operacion      "Venta" o "Alquiler"
 * @param presupuestoMax precio máximo
 * @param moneda         "USD" o "PYG"; si hay presupuesto y no se indica, se asume USD
 */
public record BusquedaRequest(
        Integer ciudadId,
        Integer barrioId,
        String categoria,
        String operacion,
        BigDecimal presupuestoMax,
        String moneda) {

    public static BusquedaRequest vacia() {
        return new BusquedaRequest(null, null, null, null, null, null);
    }
}

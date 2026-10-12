package py.sn.inmobiliaria.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de una propiedad enviados por el panel administrativo (crear o editar).
 *
 * <p>Ejemplo de JSON:</p>
 * <pre>
 * {
 *   "titulo": "Casa familiar en Area 1",
 *   "descripcion": "...", "direccion": "...",
 *   "latitud": -25.511700, "longitud": -54.611800,
 *   "idBarrio": 10011013,
 *   "tipo": "Casa",
 *   "estado": "Disponible",
 *   "casa": { "habitaciones": 3, "banos": 2, "sala": true, "comedor": true, "cocina": true,
 *             "superficieConstruida": 182, "superficieTerreno": 360 },
 *   "terreno": null,
 *   "operaciones": [ { "operacion": "Venta", "precio": 120000, "moneda": "USD" },
 *                    { "operacion": "Alquiler", "precio": 4000000, "moneda": "PYG" } ]
 * }
 * </pre>
 *
 * <p>El código público (prop-001) no se envía: lo genera el sistema al crear.</p>
 */
public record PropiedadRequest(
        String titulo,
        String descripcion,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        Integer idBarrio,
        String tipo,
        String estado,
        CasaRequest casa,
        TerrenoRequest terreno,
        List<OperacionRequest> operaciones,
        Integer idResponsable) {

    /** Sin responsable (forma anterior a la etapa 2B). */
    public PropiedadRequest(String titulo, String descripcion, String direccion, BigDecimal latitud, BigDecimal longitud,
                            Integer idBarrio, String tipo, String estado, CasaRequest casa, TerrenoRequest terreno,
                            List<OperacionRequest> operaciones) {
        this(titulo, descripcion, direccion, latitud, longitud, idBarrio, tipo, estado, casa, terreno, operaciones, null);
    }

    /** Datos de casa (obligatorios si tipo = Casa). */
    public record CasaRequest(
            Integer habitaciones,
            Integer banos,
            Boolean sala,
            Boolean comedor,
            Boolean cocina,
            BigDecimal superficieConstruida,
            BigDecimal superficieTerreno) {
    }

    /** Datos de terreno (obligatorios si tipo = Terreno). */
    public record TerrenoRequest(
            BigDecimal superficie,
            BigDecimal frente,
            BigDecimal fondo,
            Boolean tieneTitulo) {
    }

    /** Una operación con su precio: Venta USD 120000. */
    public record OperacionRequest(
            String operacion,
            BigDecimal precio,
            String moneda) {
    }
}

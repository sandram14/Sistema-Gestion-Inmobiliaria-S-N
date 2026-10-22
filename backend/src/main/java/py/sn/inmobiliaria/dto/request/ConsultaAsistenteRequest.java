package py.sn.inmobiliaria.dto.request;

/**
 * Cuerpo de POST /api/asistente/consultas.
 *
 * <pre>{ "pregunta": "¿Esta casa tiene cochera?", "codigoPropiedad": "prop-015" }</pre>
 *
 * @param pregunta        texto del visitante (máximo 500 caracteres)
 * @param codigoPropiedad opcional: la propiedad desde la que se consulta
 */
public record ConsultaAsistenteRequest(String pregunta, String codigoPropiedad) {
}

package py.sn.inmobiliaria.dto.response;

import java.util.List;

/**
 * Respuesta del asistente.
 *
 * @param respuesta               texto para mostrar al visitante
 * @param sugerirWhatsApp         true si conviene ofrecer el contacto con un agente
 * @param propiedadesRelacionadas códigos de propiedades mencionadas en la respuesta (solo códigos reales)
 */
public record AsistenteResponse(String respuesta, boolean sugerirWhatsApp, List<String> propiedadesRelacionadas) {
}

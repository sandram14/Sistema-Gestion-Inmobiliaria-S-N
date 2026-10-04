package py.sn.inmobiliaria.integration;

/**
 * Generador de respuestas de lenguaje natural. La implementación real es {@link GeminiClient};
 * las pruebas usan implementaciones en memoria, sin consumir la cuota de Google.
 */
public interface ClienteIa {

    /**
     * @param instrucciones reglas de comportamiento (instrucción de sistema)
     * @param contenido     datos y pregunta del visitante
     * @return el texto generado
     * @throws ErrorClienteIa si el servicio no responde, rechaza la petición o agotó la cuota
     */
    String generar(String instrucciones, String contenido);
}

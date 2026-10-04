package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/**
 * El asistente no puede responder ahora (HTTP 503): no está configurado, Gemini no responde
 * o se alcanzó el límite general de uso. El mensaje sugiere contactar a un agente.
 */
public class AsistenteNoDisponibleException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final String MENSAJE = "El asistente no puede responder en este momento. "
            + "Puede volver a intentarlo en unos minutos o escribir a un agente por WhatsApp.";

    public AsistenteNoDisponibleException(String mensaje) {
        super(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "ASISTENTE_NO_DISPONIBLE", mensaje);
    }
}

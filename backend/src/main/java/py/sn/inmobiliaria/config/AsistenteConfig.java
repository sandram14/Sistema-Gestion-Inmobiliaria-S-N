package py.sn.inmobiliaria.config;

import java.util.Map;

/**
 * Configuración del asistente de IA (Gemini), leída de variables de entorno.
 *
 * <p>La API key solo vive aquí, en memoria del backend: nunca se escribe en logs, respuestas
 * ni en el frontend. {@link #toString()} la oculta.</p>
 *
 * @param apiKey               GEMINI_API_KEY; null si no está configurada (el asistente responde 503)
 * @param modelo               GEMINI_MODEL (por defecto gemini-3.5-flash-lite, con nivel gratuito)
 * @param nivelRazonamiento    GEMINI_THINKING_LEVEL para modelos Gemini 3 (por defecto "minimal": no gasta
 *                             cuota en razonar); "ninguno" para no enviar el parámetro
 * @param timeoutSegundos      tiempo máximo de espera de la respuesta de Gemini
 * @param limiteMinutoSesion   consultas por minuto por visitante
 * @param limiteDiaSesion      consultas por día por visitante
 * @param limiteMinutoGlobal   consultas por minuto de todo el sitio (por debajo del límite gratuito de Google)
 * @param limiteDiaGlobal      consultas por día de todo el sitio
 * @param maxPropiedadesContexto cuántas propiedades se envían como contexto en preguntas generales
 */
public record AsistenteConfig(
        String apiKey,
        String modelo,
        String nivelRazonamiento,
        int timeoutSegundos,
        int limiteMinutoSesion,
        int limiteDiaSesion,
        int limiteMinutoGlobal,
        int limiteDiaGlobal,
        int maxPropiedadesContexto) {

    /**
     * Verificado con la clave del proyecto (octubre de 2026): los modelos gemini-2.5 ya no están disponibles
     * para claves nuevas (HTTP 404) y gemini-3.1-flash-lite respondió "saturado" (HTTP 503) de forma sostenida;
     * gemini-3.5-flash-lite responde completo y con thinkingLevel=minimal no usa razonamiento.
     */
    public static final String MODELO_POR_DEFECTO = "gemini-3.5-flash-lite";
    public static final String NIVEL_RAZONAMIENTO_POR_DEFECTO = "minimal";

    /** Lee las variables ASISTENTE_* y GEMINI_*, con valores por defecto pensados para el nivel gratuito. */
    public static AsistenteConfig desde(Map<String, String> entorno) {
        String clave = entorno.get("GEMINI_API_KEY");
        String modelo = entorno.get("GEMINI_MODEL");
        String nivel = entorno.get("GEMINI_THINKING_LEVEL");
        String nivelRazonamiento = nivel == null || nivel.isBlank() ? NIVEL_RAZONAMIENTO_POR_DEFECTO
                : "ninguno".equalsIgnoreCase(nivel.trim()) ? null : nivel.trim().toLowerCase(java.util.Locale.ROOT);
        return new AsistenteConfig(
                clave == null || clave.isBlank() ? null : clave.trim(),
                modelo == null || modelo.isBlank() ? MODELO_POR_DEFECTO : modelo.trim(),
                nivelRazonamiento,
                entero(entorno, "GEMINI_TIMEOUT_SEGUNDOS", 20),
                entero(entorno, "ASISTENTE_LIMITE_MINUTO_SESION", 10),
                entero(entorno, "ASISTENTE_LIMITE_DIA_SESION", 30),
                entero(entorno, "ASISTENTE_LIMITE_MINUTO_GLOBAL", 8),
                entero(entorno, "ASISTENTE_LIMITE_DIA_GLOBAL", 200),
                entero(entorno, "ASISTENTE_MAX_PROPIEDADES_CONTEXTO", 60));
    }

    public boolean configurado() {
        return apiKey != null;
    }

    private static int entero(Map<String, String> entorno, String nombre, int porDefecto) {
        String v = entorno.get(nombre);
        if (v == null || v.isBlank()) {
            return porDefecto;
        }
        try {
            int n = Integer.parseInt(v.trim());
            return n > 0 ? n : porDefecto;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    @Override
    public String toString() {
        return "AsistenteConfig[apiKey=" + (apiKey == null ? "no configurada" : "****") + ", modelo=" + modelo
                + ", razonamiento=" + (nivelRazonamiento == null ? "sin ajuste" : nivelRazonamiento)
                + ", timeout=" + timeoutSegundos + "s, limites=" + limiteMinutoSesion + "/min y " + limiteDiaSesion
                + "/día por visitante, " + limiteMinutoGlobal + "/min y " + limiteDiaGlobal + "/día en total]";
    }
}

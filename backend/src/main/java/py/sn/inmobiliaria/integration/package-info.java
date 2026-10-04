/**
 * Integraciones con servicios externos: el cliente de Gemini.
 * La API key se lee de una variable de entorno, viaja solo en la cabecera x-goog-api-key
 * (nunca en la URL) y no sale del backend.
 */
package py.sn.inmobiliaria.integration;

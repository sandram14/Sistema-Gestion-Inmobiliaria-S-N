package py.sn.inmobiliaria.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Peticiones y respuestas de clientes potenciales. */
public final class ClienteDtos {

    private ClienteDtos() {
    }

    /**
     * Formulario público (chat o página Contacto). aceptaTratamiento debe ser true y versionConsentimiento la vigente.
     * canal: "CHAT" (por defecto) o "CONTACTO".
     */
    public record Solicitud(String nombre, String telefono, String email, String mensaje, String codigoPropiedad,
                            Boolean aceptaTratamiento, String versionConsentimiento, String canal) {

        /** Sin canal: chat (forma anterior). */
        public Solicitud(String nombre, String telefono, String email, String mensaje, String codigoPropiedad,
                         Boolean aceptaTratamiento, String versionConsentimiento) {
            this(nombre, telefono, email, mensaje, codigoPropiedad, aceptaTratamiento, versionConsentimiento, null);
        }
    }

    /** Alta manual desde el panel. origen: WHATSAPP, LLAMADA u OTRO. */
    public record Manual(String nombre, String telefono, String email, String interes, String codigoPropiedad,
                         String origen, Integer idResponsable) {
    }

    public record Ref(long id, String codigo, String titulo) {
    }

    public record Persona(int id, String nombre, boolean activo) {
    }

    public record Resumen(long id, String nombre, String contacto, String interes, Ref propiedad, String origen,
                          String estado, Persona responsable, OffsetDateTime creadoEn) {
    }

    public record Detalle(long id, String nombre, String telefono, String email, String interes, Ref propiedad,
                          Long idConsulta, String origen, String estado, Persona responsable, Persona registradoPor,
                          OffsetDateTime consentimientoEn, String consentimientoVersion, OffsetDateTime creadoEn,
                          OffsetDateTime actualizadoEn) {
    }

    public record Conteos(long total, long pendientes, long contactados, long descartados) {
    }

    public record Pagina(List<Resumen> elementos, long total, int pagina, int tamano, Conteos conteos) {
    }
}

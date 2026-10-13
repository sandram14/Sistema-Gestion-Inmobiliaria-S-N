package py.sn.inmobiliaria.dto.response.admin;

import py.sn.inmobiliaria.service.consultas.AnalisisConsultas;

import java.time.OffsetDateTime;
import java.util.List;

/** Respuestas de las consultas del chat IA en el panel. Nunca incluyen la sesión ni datos del visitante. */
public final class ConsultaDtos {

    private ConsultaDtos() {
    }

    public record PropiedadRef(long id, String codigo, String titulo) {
    }

    /**
     * @param registroCompleto false en las consultas anteriores a la migración 003: su resultado y
     *                         sus recomendadas no se registraban (no se deducen ni se inventan)
     */
    public record Resumen(long id, OffsetDateTime fechaHora, String pregunta, PropiedadRef propiedad, String resultado,
                          String estadoRevision, boolean registroCompleto) {
    }

    /** Una página del historial. */
    public record Pagina(List<Resumen> elementos, long total, int pagina, int tamano) {
    }

    public record ItemConversacion(long id, OffsetDateTime fechaHora, String pregunta, boolean actual) {
    }

    public record Detalle(long id, OffsetDateTime fechaHora, String pregunta, String respuesta, PropiedadRef propiedad,
                          List<PropiedadRef> recomendadas, String resultado, boolean registroCompleto,
                          String estadoRevision, String revisadaPor, OffsetDateTime revisadaEn,
                          List<ItemConversacion> conversacion) {
    }

    /** Análisis del periodo. "comprobado" e "inferido" se indican en cada bloque del panel. */
    public record Analisis(OffsetDateTime desde, OffsetDateTime hasta, AnalisisConsultas.Resultado indicadores,
                           List<Resumen> sinServicio, List<Resumen> derivadas, List<Resumen> sinInformacionInferida,
                           List<Resumen> sinInformacionUtil, List<Resumen> fueraDeAmbito) {
    }
}

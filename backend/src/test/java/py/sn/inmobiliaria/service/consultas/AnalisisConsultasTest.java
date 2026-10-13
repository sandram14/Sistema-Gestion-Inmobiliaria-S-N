package py.sn.inmobiliaria.service.consultas;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.service.consultas.AnalisisConsultas.Conteo;
import py.sn.inmobiliaria.service.consultas.AnalisisConsultas.Fila;
import py.sn.inmobiliaria.service.consultas.AnalisisConsultas.Ref;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalisisConsultasTest {

    static final String RESPALDO = "El asistente no puede responder en este momento.";
    static final Ref CASA = new Ref(1, "prop-001", "Casa en Area 1");
    static final Ref TERRENO = new Ref(2, "terr-001", "Terreno en Centro");
    static final OffsetDateTime F = OffsetDateTime.parse("2026-10-05T10:00:00-03:00");

    static Fila f(long id, String sesion, String pregunta, String respuesta, Ref prop, String resultado, List<Ref> recs) {
        return new Fila(id, F, sesion, pregunta, respuesta, prop, prop == null ? null : (prop == CASA ? "Casa" : "Terreno"),
                prop == null ? null : "CIUDAD DEL ESTE", prop == null ? null : "AREA 1", resultado, recs,
                prop == CASA ? List.of(new AnalisisConsultas.Precio("Venta", new java.math.BigDecimal("45000"), "USD"),
                        new AnalisisConsultas.Precio("Alquiler", new java.math.BigDecimal("4000000"), "PYG"))
                        : prop == TERRENO ? List.of(new AnalisisConsultas.Precio("Venta", new java.math.BigDecimal("150000"), "USD")) : List.of());
    }

    final List<Fila> filas = List.of(
            // Consultas ANTIGUAS (sin resultado registrado)
            f(1, "s1", "¿Cuánto cuesta?", "Cuesta 45.000 USD.", CASA, null, List.of()),
            f(2, "s1", "¿Tiene cochera?", "No dispongo de esa información.", CASA, null, List.of()),
            f(3, "s2", "cuanto cuesta", RESPALDO, null, null, List.of()),
            f(4, "s2", "Busco casas en alquiler en Hernandarias", "Le recomiendo…", null, null, List.of()),
            // Consultas NUEVAS (con resultado registrado)
            f(5, "s3", "Quiero visitar el terreno", "Un asesor puede ayudarle.", TERRENO, "DERIVADA_ASESOR", List.of(TERRENO)),
            f(6, "s4", "Terrenos en Ciudad del Este", "- terr-001…", null, "RESPONDIDA", List.of(TERRENO, CASA)),
            f(7, "s4", "¿Y la cochera?", "No dispongo de esa información.", null, "RESPONDIDA", List.of()),
            f(8, "s5", "¿Quién ganó el mundial?", "Solo puedo ayudarle con propiedades.", null, "FUERA_DE_AMBITO", List.of()));

    final AnalisisConsultas.Resultado r = AnalisisConsultas.calcular(filas, List.of("CIUDAD DEL ESTE", "HERNANDARIAS", "AREA 1"), RESPALDO);

    @Test
    void totalesComprobados() {
        assertEquals(8, r.consultas());
        assertEquals(5, r.conversaciones());
        assertEquals(3, r.conPropiedad());
        assertEquals(4, r.conRegistroCompleto());
    }

    @Test
    void sinServicioSeComprueba_EnLasAntiguasSoloPorElMensajeDeRespaldoExacto() {
        assertEquals(List.of(3L), r.sinServicioIds());
        assertEquals(1, r.sinServicio());
    }

    @Test
    void lasDerivadasSoloSeCuentanCuandoElResultadoEstaRegistrado() {
        assertEquals(List.of(5L), r.derivadasIds()); // nunca se deduce para las antiguas
    }

    @Test
    void sinInformacionSeInfiereSoloEnLasAntiguas() {
        assertEquals(List.of(2L), r.sinInformacionInferidaIds()); // la 7 tiene resultado registrado: no se infiere
    }

    @Test
    void preguntasRepetidasSinImportarMayusculasNiAcentos() {
        assertEquals(List.of(new Conteo("¿Cuánto cuesta?", 2)), r.preguntasRepetidas());
    }

    @Test
    void propiedadesYZonasComprobadasSalenDeLaPropiedadConsultada() {
        assertEquals(CASA, r.propiedadesConsultadas().get(0).propiedad());
        assertEquals(2, r.propiedadesConsultadas().get(0).cantidad());
        assertEquals(List.of(new Conteo("Casa", 2), new Conteo("Terreno", 1)), r.tiposComprobados());
        assertEquals(List.of(new Conteo("CIUDAD DEL ESTE", 3)), r.zonasComprobadas());
    }

    @Test
    void temasTiposYZonasInferidosDelTexto() {
        assertTrue(r.temasInferidos().contains(new Conteo("Precio", 2)));
        assertTrue(r.temasInferidos().contains(new Conteo("Características", 2)));
        assertTrue(r.temasInferidos().contains(new Conteo("Visitas", 1)));
        // Tipos y zonas inferidos: solo de preguntas generales (sin propiedad consultada)
        assertEquals(List.of(new Conteo("Casa", 1), new Conteo("Terreno", 1)), r.tiposInferidos());
        assertTrue(r.zonasInferidas().contains(new Conteo("HERNANDARIAS", 1)));
        assertTrue(r.zonasInferidas().contains(new Conteo("CIUDAD DEL ESTE", 1)));
    }

    @Test
    void recomendadasSoloDeLasRegistradas() {
        assertEquals(TERRENO, r.masRecomendadas().get(0).propiedad());
        assertEquals(2, r.masRecomendadas().get(0).cantidad());
    }

    @Test
    void sinConsultasTodoQuedaEnCero() {
        var vacio = AnalisisConsultas.calcular(List.of(), List.of(), RESPALDO);
        assertEquals(0, vacio.consultas());
        assertTrue(vacio.temasInferidos().isEmpty());
    }

    @Test
    void lasZonasSeBuscanComoPalabrasCompletas() {
        assertFalse(AnalisisConsultas.contienePalabra("arealmente", "AREA"));
        assertTrue(AnalisisConsultas.contienePalabra("en Área 1 hay casas", "AREA 1"));
    }

    @Test
    void rangosDePrecioPorOperacionYMonedaSinConvertir() {
        assertEquals(List.of(
                new AnalisisConsultas.Rango("Venta", "USD", "Menos de USD 50.000", 2),            // la casa, consultada 2 veces
                new AnalisisConsultas.Rango("Venta", "USD", "USD 100.000 a USD 200.000", 1),      // el terreno
                new AnalisisConsultas.Rango("Alquiler", "PYG", "PYG 4.000.000 a PYG 7.000.000", 2)), // la casa, en guaraníes
                r.rangosPrecio());
    }

    @Test
    void caracteristicasSolicitadasInferidas() {
        assertTrue(r.caracteristicasInferidas().contains(new Conteo("Cochera", 2)));
    }

    @Test
    void calidad_FueraDelAmbitoSoloConResultadoRegistrado_LasAntiguasNoSeClasifican() {
        var c = r.calidad();
        assertEquals(List.of(8L), c.fueraDeAmbitoIds());
        assertEquals(4, c.fueraDeAmbitoNoRegistrado());          // las 4 antiguas: "No registrado"
        assertEquals(List.of(2L, 7L), c.sinInformacionIds());    // inferido, en antiguas y nuevas
        assertEquals(List.of(3L), c.fallosServicioIds());
    }
}

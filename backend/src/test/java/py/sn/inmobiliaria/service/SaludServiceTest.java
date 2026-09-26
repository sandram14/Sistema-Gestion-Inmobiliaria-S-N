package py.sn.inmobiliaria.service;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.response.HealthResponse;
import py.sn.inmobiliaria.repository.PropiedadDao;
import py.sn.inmobiliaria.repository.SaludBaseDatosDao;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Prueba el service con DAOs "de mentira" (implementaciones en memoria):
 * no necesita una base de datos real.
 */
class SaludServiceTest {

    private final AppConfig config = AppConfig.cargar();

    private final SaludBaseDatosDao baseOk = () -> new SaludBaseDatosDao.InfoBaseDatos("16.4", "3.4.2");
    private final PropiedadDao jpaOk = () -> 28L;

    @Test
    void conBaseEHibernateFuncionandoElEstadoEsOk() {
        HealthResponse r = new SaludService(config, baseOk, jpaOk).verificar();

        assertEquals("ok", r.estado());
        assertEquals("ok", r.baseDatos().estado());
        assertEquals("16.4", r.baseDatos().postgresql());
        assertEquals("3.4.2", r.baseDatos().postgis());
        assertEquals("ok", r.jpa().estado());
        assertEquals(28L, r.jpa().propiedadesDisponibles());
    }

    @Test
    void siLaBaseFallaElEstadoEsDegradadoSinDetallesInternos() {
        SaludBaseDatosDao baseCaida = () -> {
            throw new SQLException("Connection refused: localhost:5433");
        };

        HealthResponse r = new SaludService(config, baseCaida, jpaOk).verificar();

        assertEquals("degradado", r.estado());
        assertEquals("error", r.baseDatos().estado());
        assertNull(r.baseDatos().postgresql());
        assertEquals("error", r.jpa().estado());
    }

    @Test
    void siHibernateFallaElEstadoEsDegradado() {
        PropiedadDao jpaRoto = () -> {
            throw new IllegalStateException("Schema-validation: missing column");
        };

        HealthResponse r = new SaludService(config, baseOk, jpaRoto).verificar();

        assertEquals("degradado", r.estado());
        assertEquals("ok", r.baseDatos().estado());
        assertEquals("error", r.jpa().estado());
        assertNull(r.jpa().propiedadesDisponibles());
    }

    @Test
    void sinBaseConfiguradaSeInformaNoConfigurada() {
        HealthResponse r = new SaludService(config, null, null).verificar();

        assertEquals("degradado", r.estado());
        assertEquals("no_configurada", r.baseDatos().estado());
        assertEquals("no_configurada", r.jpa().estado());
    }
}

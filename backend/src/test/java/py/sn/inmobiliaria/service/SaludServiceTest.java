package py.sn.inmobiliaria.service;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.response.HealthResponse;
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

    @Test
    void conBaseRespondiendoElEstadoEsOk() {
        SaludBaseDatosDao daoOk = () -> new SaludBaseDatosDao.InfoBaseDatos("16.4", "3.4.2");

        HealthResponse r = new SaludService(config, daoOk).verificar();

        assertEquals("ok", r.estado());
        assertEquals("ok", r.baseDatos().estado());
        assertEquals("16.4", r.baseDatos().postgresql());
        assertEquals("3.4.2", r.baseDatos().postgis());
    }

    @Test
    void siLaBaseFallaElEstadoEsDegradadoSinDetallesInternos() {
        SaludBaseDatosDao daoCaido = () -> {
            throw new SQLException("Connection refused: localhost:5433");
        };

        HealthResponse r = new SaludService(config, daoCaido).verificar();

        assertEquals("degradado", r.estado());
        assertEquals("error", r.baseDatos().estado());
        assertNull(r.baseDatos().postgresql());
    }

    @Test
    void sinBaseConfiguradaSeInformaNoConfigurada() {
        HealthResponse r = new SaludService(config, null).verificar();

        assertEquals("degradado", r.estado());
        assertEquals("no_configurada", r.baseDatos().estado());
    }
}

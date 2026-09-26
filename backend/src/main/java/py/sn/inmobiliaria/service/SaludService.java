package py.sn.inmobiliaria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.response.HealthResponse;
import py.sn.inmobiliaria.repository.SaludBaseDatosDao;

import java.time.OffsetDateTime;

/**
 * Arma el estado de salud del backend: la aplicación y su base de datos.
 */
public class SaludService {

    private static final Logger log = LoggerFactory.getLogger(SaludService.class);

    private final AppConfig config;
    private final SaludBaseDatosDao saludDao; // null si la base no está configurada

    public SaludService(AppConfig config, SaludBaseDatosDao saludDao) {
        this.config = config;
        this.saludDao = saludDao;
    }

    public HealthResponse verificar() {
        HealthResponse.EstadoBaseDatos bd = estadoBaseDatos();
        String estadoGeneral = HealthResponse.EstadoBaseDatos.OK.equals(bd.estado()) ? "ok" : "degradado";
        return new HealthResponse(
                estadoGeneral,
                config.nombre(),
                config.version(),
                config.entorno(),
                System.getProperty("java.version"),
                OffsetDateTime.now(),
                bd);
    }

    private HealthResponse.EstadoBaseDatos estadoBaseDatos() {
        if (saludDao == null) {
            return HealthResponse.EstadoBaseDatos.noConfigurada();
        }
        try {
            SaludBaseDatosDao.InfoBaseDatos info = saludDao.consultar();
            return HealthResponse.EstadoBaseDatos.ok(info.postgresql(), info.postgis());
        } catch (Exception e) {
            // El detalle solo va al log; la respuesta HTTP no revela información interna.
            log.warn("La base de datos no responde: {}", e.getMessage());
            return HealthResponse.EstadoBaseDatos.error();
        }
    }
}

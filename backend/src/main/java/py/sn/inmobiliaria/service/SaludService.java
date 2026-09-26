package py.sn.inmobiliaria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.sn.inmobiliaria.config.AppConfig;
import py.sn.inmobiliaria.dto.response.HealthResponse;
import py.sn.inmobiliaria.dto.response.HealthResponse.EstadoBaseDatos;
import py.sn.inmobiliaria.dto.response.HealthResponse.EstadoJpa;
import py.sn.inmobiliaria.repository.PropiedadDao;
import py.sn.inmobiliaria.repository.SaludBaseDatosDao;

import java.time.OffsetDateTime;

/**
 * Arma el estado de salud del backend: la aplicación, la base de datos y Hibernate.
 */
public class SaludService {

    private static final Logger log = LoggerFactory.getLogger(SaludService.class);

    private final AppConfig config;
    private final SaludBaseDatosDao saludDao;   // null si la base no está configurada
    private final PropiedadDao propiedadDao;    // null si la base no está configurada

    public SaludService(AppConfig config, SaludBaseDatosDao saludDao, PropiedadDao propiedadDao) {
        this.config = config;
        this.saludDao = saludDao;
        this.propiedadDao = propiedadDao;
    }

    public HealthResponse verificar() {
        EstadoBaseDatos bd = estadoBaseDatos();
        // Si la base no responde, no tiene sentido consultar con Hibernate.
        EstadoJpa jpa = EstadoBaseDatos.OK.equals(bd.estado()) ? estadoJpa()
                : (propiedadDao == null ? EstadoJpa.noConfigurada() : EstadoJpa.error());

        boolean todoOk = EstadoBaseDatos.OK.equals(bd.estado()) && EstadoBaseDatos.OK.equals(jpa.estado());
        return new HealthResponse(
                todoOk ? "ok" : "degradado",
                config.nombre(),
                config.version(),
                config.entorno(),
                System.getProperty("java.version"),
                OffsetDateTime.now(),
                bd,
                jpa);
    }

    private EstadoBaseDatos estadoBaseDatos() {
        if (saludDao == null) {
            return EstadoBaseDatos.noConfigurada();
        }
        try {
            SaludBaseDatosDao.InfoBaseDatos info = saludDao.consultar();
            return EstadoBaseDatos.ok(info.postgresql(), info.postgis());
        } catch (Exception e) {
            // El detalle solo va al log; la respuesta HTTP no revela información interna.
            log.warn("La base de datos no responde: {}", e.getMessage());
            return EstadoBaseDatos.error();
        }
    }

    private EstadoJpa estadoJpa() {
        if (propiedadDao == null) {
            return EstadoJpa.noConfigurada();
        }
        try {
            return EstadoJpa.ok(propiedadDao.contarDisponibles());
        } catch (Exception e) {
            log.error("Hibernate no pudo leer las entidades", e);
            return EstadoJpa.error();
        }
    }
}

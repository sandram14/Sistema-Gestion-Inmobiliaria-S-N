package py.sn.inmobiliaria.service;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import py.sn.inmobiliaria.config.BaseDatosConfig;
import py.sn.inmobiliaria.config.DataSourceFactory;
import py.sn.inmobiliaria.config.JpaProvider;
import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.request.PropiedadRequest;
import py.sn.inmobiliaria.dto.request.UsuarioRequest;
import py.sn.inmobiliaria.exception.AccesoDenegadoException;
import py.sn.inmobiliaria.exception.ConflictoException;
import py.sn.inmobiliaria.prueba.DatosPrueba;
import py.sn.inmobiliaria.prueba.DatosPrueba.Zona;
import py.sn.inmobiliaria.repository.impl.JdbcUbicacionDao;
import py.sn.inmobiliaria.repository.impl.JpaCatalogoDao;
import py.sn.inmobiliaria.repository.impl.JpaImagenDao;
import py.sn.inmobiliaria.repository.impl.JpaPropiedadDao;
import py.sn.inmobiliaria.repository.impl.JpaUsuarioDao;
import py.sn.inmobiliaria.seguridad.ContrasenaHasher;
import py.sn.inmobiliaria.seguridad.UsuarioSesion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Usuarios y responsable de propiedades contra la base real (solo si DB_URL está definida).
 * Requiere la migración 002. Borra al terminar los usuarios y propiedades que crea.
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class UsuariosIntegracionTest {

    private static HikariDataSource dataSource;
    private static JpaProvider jpa;
    private static UsuarioService usuarios;
    private static PropiedadAdminService propiedades;
    private static Zona zona;
    private final String marca = UUID.randomUUID().toString().substring(0, 8);

    @BeforeAll
    static void iniciar() {
        dataSource = DataSourceFactory.crear(new BaseDatosConfig(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")));
        jpa = new JpaProvider(dataSource);
        TxRunner tx = new TxRunner(jpa);
        JpaCatalogoDao catalogoDao = new JpaCatalogoDao();
        usuarios = new UsuarioService(tx, new JpaUsuarioDao(), catalogoDao, new ContrasenaHasher());
        propiedades = new PropiedadAdminService(tx, new JpaPropiedadDao(), catalogoDao, new JpaImagenDao(),
                new JdbcUbicacionDao(dataSource));
        zona = DatosPrueba.zona(dataSource);
    }

    @AfterEach
    void limpiar() {
        String patron = "%" + marca + "%";
        DatosPrueba.numero(dataSource, "WITH b AS (DELETE FROM propiedad WHERE titulo LIKE ? RETURNING 1) SELECT count(*) FROM b", patron);
        DatosPrueba.numero(dataSource, "WITH b AS (DELETE FROM usuario WHERE email LIKE ? RETURNING 1) SELECT count(*) FROM b", patron);
    }

    @AfterAll
    static void cerrar() {
        if (jpa != null) {
            jpa.close();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private UsuarioRequest req(String nombre, String rol) {
        return new UsuarioRequest(nombre, "Prueba", nombre.toLowerCase() + "." + marca + "@prueba.test", null, null, rol);
    }

    private PropiedadRequest casa() {
        return new PropiedadRequest("Casa " + marca, null, null, zona.latitud(), zona.longitud(), zona.idBarrio(), "Casa", null,
                new PropiedadRequest.CasaRequest(2, 1, true, true, true, new BigDecimal("90"), null), null,
                List.of(new PropiedadRequest.OperacionRequest("Venta", new BigDecimal("1000"), "PYG")));
    }

    private static UsuarioSesion sesion(int id, String rol) {
        return new UsuarioSesion(id, "X", "Y", "x@prueba.test", rol);
    }

    @Test
    void altaConContrasenaTemporalYEmailUnico() {
        var creado = usuarios.crear(req("Vendedor", "VENDEDOR"));
        assertTrue(creado.usuario().debeCambiarPassword());
        assertEquals("VENDEDOR", creado.usuario().rol());
        String hash = String.valueOf(DatosPrueba.numero(dataSource,
                "SELECT CASE WHEN password_hash <> ? THEN 1 ELSE 0 END FROM usuario WHERE id_usuario = ?",
                creado.contrasenaTemporal(), creado.usuario().id()));
        assertEquals("1", hash); // se guarda el hash, nunca la contraseña
        assertThrows(ConflictoException.class, () -> usuarios.crear(new UsuarioRequest("Otro", "Prueba",
                creado.usuario().email().toUpperCase(), null, null, "GERENTE")));
    }

    @Test
    void desactivarYReactivar() {
        int id = usuarios.crear(req("Gerente", "GERENTE")).usuario().id();
        assertFalse(usuarios.cambiarEstado(id, false, -1).activo());
        assertTrue(usuarios.cambiarEstado(id, true, -1).activo());
    }

    @Test
    void elVendedorSoloEditaLasPropiedadesAsignadasAEl() {
        int dueno = usuarios.crear(req("Dueno", "VENDEDOR")).usuario().id();
        int otro = usuarios.crear(req("Otro", "VENDEDOR")).usuario().id();
        long id = propiedades.crear(casa(), dueno).id();

        assertDoesNotThrow(() -> propiedades.verificarPuedeEditar(id, sesion(dueno, "VENDEDOR")));
        assertThrows(AccesoDenegadoException.class, () -> propiedades.verificarPuedeEditar(id, sesion(otro, "VENDEDOR")));
        assertDoesNotThrow(() -> propiedades.verificarPuedeEditar(id, sesion(otro, "GERENTE")));

        propiedades.actualizar(id, casa(), true, otro);                    // el gerente reasigna
        assertEquals(1, DatosPrueba.numero(dataSource,
                "SELECT count(*) FROM propiedad WHERE id_propiedad = ? AND id_responsable = ?", id, otro));
        assertEquals(1L, usuarios.obtener(otro).propiedadesAsignadas());
    }
}

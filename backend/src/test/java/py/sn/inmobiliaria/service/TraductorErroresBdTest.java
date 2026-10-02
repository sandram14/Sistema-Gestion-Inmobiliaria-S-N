package py.sn.inmobiliaria.service;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import py.sn.inmobiliaria.exception.ApiException;
import py.sn.inmobiliaria.exception.ConflictoException;
import py.sn.inmobiliaria.exception.ReglaNegocioException;
import py.sn.inmobiliaria.exception.ServicioNoDisponibleException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Simula los errores que envía PostgreSQL (mismo formato que el servidor real)
 * envueltos como lo hace Hibernate.
 */
class TraductorErroresBdTest {

    /** Error de PostgreSQL con código SQLSTATE, restricción y mensaje. */
    private static RuntimeException errorPostgres(String sqlState, String restriccion, String mensaje) {
        String crudo = "SERROR\0C" + sqlState + "\0M" + mensaje + "\0n" + restriccion + "\0";
        PSQLException psql = new PSQLException(new ServerErrorMessage(crudo));
        return new RuntimeException("envoltorio de Hibernate", psql);
    }

    @Test
    void triggerDeReglaDeNegocioSeConvierteEn422ConSuMensaje() {
        RuntimeException r = TraductorErroresBd.traducir(errorPostgres("23514", "regla_terreno_solo_venta",
                "Un terreno solamente puede tener la operación Venta"));

        ReglaNegocioException e = assertInstanceOf(ReglaNegocioException.class, r);
        assertEquals(422, e.estadoHttp());
        assertEquals("REGLA_TERRENO_SOLO_VENTA", e.codigo());
        assertEquals("Un terreno solamente puede tener la operación Venta", e.getMessage());
    }

    @Test
    void checkTecnicoNoExponeDetallesInternos() {
        RuntimeException r = TraductorErroresBd.traducir(errorPostgres("23514", "ck_operacion_moneda", "detalle interno"));

        ApiException e = assertInstanceOf(ApiException.class, r);
        assertEquals("RESTRICCION", e.codigo());
        assertEquals(false, e.getMessage().contains("ck_operacion_moneda"));
    }

    @Test
    void codigoRepetidoEs409() {
        RuntimeException r = TraductorErroresBd.traducir(errorPostgres("23505", "propiedad_codigo_key", "duplicate key"));

        ConflictoException e = assertInstanceOf(ConflictoException.class, r);
        assertEquals(409, e.estadoHttp());
        assertEquals("Ya existe una propiedad con ese código", e.getMessage());
    }

    @Test
    void baseCaidaEs503() {
        RuntimeException conexion = new RuntimeException(new SQLException("Connection refused", "08001"));

        assertInstanceOf(ServicioNoDisponibleException.class, TraductorErroresBd.traducir(conexion));
    }

    @Test
    void erroresQueNoSonDeBaseSeDevuelvenSinCambios() {
        IllegalStateException otro = new IllegalStateException("otro problema");

        assertSame(otro, TraductorErroresBd.traducir(otro));
    }
}

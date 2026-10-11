package py.sn.inmobiliaria.seguridad;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasAccesoTest {

    private static Optional<Permiso> p(String metodo, String ruta) {
        return ReglasAcceso.permisoPara(metodo, ruta);
    }

    @Test
    void cadaEndpointAdministrativoExigeSuPermiso() {
        assertEquals(Optional.of(Permiso.CUENTA_PROPIA), p("GET", "/api/admin/auth/me"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_VER), p("GET", "/api/admin/propiedades"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_VER), p("GET", "/api/admin/propiedades/12"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_EDITAR), p("POST", "/api/admin/propiedades"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_EDITAR), p("PUT", "/api/admin/propiedades/12"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_BAJA), p("DELETE", "/api/admin/propiedades/12"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_EDITAR), p("POST", "/api/admin/propiedades/12/imagenes"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_EDITAR), p("DELETE", "/api/admin/propiedades/12/imagenes/3"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_EDITAR), p("PATCH", "/api/admin/propiedades/12/imagenes/3/principal"));
        assertEquals(Optional.of(Permiso.PROPIEDADES_EDITAR), p("GET", "/api/admin/ubicacion"));
    }

    @Test
    void lasRutasSinReglaNoTienenPermisoYQuedanBloqueadas() {
        assertTrue(p("GET", "/api/admin/usuarios").isEmpty());          // todavía no existe
        assertTrue(p("PUT", "/api/admin/propiedades").isEmpty());       // método no previsto
        assertTrue(p("DELETE", "/api/admin/propiedades/abc").isEmpty()); // id no numérico
        assertTrue(p("GET", "/api/admin/propiedades/12/../usuarios").isEmpty());
    }

    @Test
    void ignoraMayusculasEnElMetodoYLaBarraFinal() {
        assertEquals(Optional.of(Permiso.PROPIEDADES_VER), p("get", "/api/admin/propiedades/"));
    }
}

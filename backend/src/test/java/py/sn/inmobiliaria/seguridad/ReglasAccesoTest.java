package py.sn.inmobiliaria.seguridad;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(p("DELETE", "/api/admin/usuarios/3").isEmpty());     // los usuarios no se borran
        assertTrue(p("PUT", "/api/admin/propiedades").isEmpty());       // método no previsto
        assertTrue(p("DELETE", "/api/admin/propiedades/abc").isEmpty()); // id no numérico
        assertTrue(p("GET", "/api/admin/propiedades/12/../usuarios").isEmpty());
    }

    @Test
    void ignoraMayusculasEnElMetodoYLaBarraFinal() {
        assertEquals(Optional.of(Permiso.PROPIEDADES_VER), p("get", "/api/admin/propiedades/"));
    }

    @Test
    void usuariosYCuenta() {
        assertEquals(Optional.of(Permiso.USUARIOS_VER), p("GET", "/api/admin/usuarios"));
        assertEquals(Optional.of(Permiso.USUARIOS_VER), p("GET", "/api/admin/usuarios/3"));
        assertEquals(Optional.of(Permiso.USUARIOS_GESTIONAR), p("POST", "/api/admin/usuarios"));
        assertEquals(Optional.of(Permiso.USUARIOS_GESTIONAR), p("PUT", "/api/admin/usuarios/3"));
        assertEquals(Optional.of(Permiso.USUARIOS_GESTIONAR), p("PATCH", "/api/admin/usuarios/3/estado"));
        assertEquals(Optional.of(Permiso.USUARIOS_GESTIONAR), p("POST", "/api/admin/usuarios/3/contrasena-temporal"));
        for (String[] r : new String[][] {{"GET", "/api/admin/cuenta"}, {"PUT", "/api/admin/cuenta"},
                {"PUT", "/api/admin/cuenta/email"}, {"PUT", "/api/admin/cuenta/contrasena"}}) {
            assertEquals(Optional.of(Permiso.CUENTA_PROPIA), p(r[0], r[1]));
        }
    }

    @Test
    void conContrasenaTemporalSoloSePuedeCambiarla() {
        assertTrue(ReglasAcceso.permitidaConContrasenaTemporal("PUT", "/api/admin/cuenta/contrasena"));
        assertTrue(ReglasAcceso.permitidaConContrasenaTemporal("GET", "/api/admin/auth/me"));
        assertTrue(ReglasAcceso.permitidaConContrasenaTemporal("GET", "/api/admin/cuenta"));
        assertFalse(ReglasAcceso.permitidaConContrasenaTemporal("GET", "/api/admin/propiedades"));
        assertFalse(ReglasAcceso.permitidaConContrasenaTemporal("PUT", "/api/admin/cuenta/email"));
    }

    @Test
    void consultasDelChat() {
        assertEquals(Optional.of(Permiso.CONSULTAS_VER), p("GET", "/api/admin/consultas"));
        assertEquals(Optional.of(Permiso.CONSULTAS_VER), p("GET", "/api/admin/consultas/12"));
        assertEquals(Optional.of(Permiso.CONSULTAS_VER), p("PATCH", "/api/admin/consultas/12/revision"));
        assertEquals(Optional.of(Permiso.CONSULTAS_VER_TODAS), p("GET", "/api/admin/consultas/analisis"));
        assertTrue(p("DELETE", "/api/admin/consultas/12").isEmpty()); // las consultas no se borran desde el panel
    }

    @Test
    void clientesPotenciales() {
        assertEquals(Optional.of(Permiso.CLIENTES_VER), p("GET", "/api/admin/clientes-potenciales"));
        assertEquals(Optional.of(Permiso.CLIENTES_VER), p("GET", "/api/admin/clientes-potenciales/3"));
        assertEquals(Optional.of(Permiso.CLIENTES_VER), p("POST", "/api/admin/clientes-potenciales"));
        assertEquals(Optional.of(Permiso.CLIENTES_VER), p("PATCH", "/api/admin/clientes-potenciales/3/estado"));
        assertEquals(Optional.of(Permiso.CLIENTES_ASIGNAR), p("PATCH", "/api/admin/clientes-potenciales/3/responsable"));
        assertTrue(p("DELETE", "/api/admin/clientes-potenciales/3").isEmpty());
    }

    @Test
    void gestionDelSitio() {
        assertEquals(Optional.of(Permiso.CONTENIDO_EDITAR), p("GET", "/api/admin/sitio/secciones/inicio"));
        assertEquals(Optional.of(Permiso.CONTENIDO_EDITAR), p("PUT", "/api/admin/sitio/secciones/contacto"));
        assertEquals(Optional.of(Permiso.CONTENIDO_EDITAR), p("POST", "/api/admin/sitio/imagenes"));
        assertEquals(Optional.of(Permiso.OFERTAS_GESTIONAR), p("PUT", "/api/admin/sitio/ofertas"));
        assertTrue(p("PUT", "/api/admin/sitio/secciones/precios").isEmpty()); // sección inexistente: bloqueada
    }
}

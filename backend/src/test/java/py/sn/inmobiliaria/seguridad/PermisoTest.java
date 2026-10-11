package py.sn.inmobiliaria.seguridad;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static py.sn.inmobiliaria.seguridad.Permiso.*;

/** La matriz de permisos acordada, rol por rol. */
class PermisoTest {

    @Test
    void elAdministradorTieneTodosLosPermisos() {
        assertEquals(EnumSet.allOf(Permiso.class), Permiso.deRol(ADMIN));
    }

    @Test
    void elGerenteGestionaPropiedadesYContenidoPeroNoUsuariosNiConfiguracion() {
        Set<Permiso> g = Permiso.deRol(GERENTE);
        for (Permiso p : new Permiso[] {PROPIEDADES_VER, PROPIEDADES_EDITAR, PROPIEDADES_EDITAR_TODAS, PROPIEDADES_BAJA,
                OFERTAS_GESTIONAR, CONTENIDO_EDITAR, CONFIGURACION_VER, CONSULTAS_VER, CONSULTAS_VER_TODAS,
                ESTADISTICAS_VER, USUARIOS_VER, CUENTA_PROPIA}) {
            assertTrue(g.contains(p), p.name());
        }
        assertFalse(g.contains(USUARIOS_GESTIONAR));
        assertFalse(g.contains(CONFIGURACION_EDITAR));
    }

    @Test
    void elVendedorSoloVePropiedadesEditaLasSuyasYVeConsultas() {
        assertEquals(EnumSet.of(PROPIEDADES_VER, PROPIEDADES_EDITAR, CONSULTAS_VER, CUENTA_PROPIA), Permiso.deRol(VENDEDOR));
    }

    @Test
    void unRolDesconocidoNoTienePermisos() {
        assertTrue(Permiso.deRol("INVITADO").isEmpty());
        assertTrue(Permiso.deRol(null).isEmpty());
        assertFalse(Permiso.esRolValido("INVITADO"));
    }

    @Test
    void todosLosRolesPuedenUsarSuCuenta() {
        for (String rol : Permiso.ROLES) {
            assertTrue(Permiso.deRol(rol).contains(CUENTA_PROPIA), rol);
        }
    }

    @Test
    void laSesionIncluyeLosPermisosDelRol() {
        UsuarioSesion v = new UsuarioSesion(3, "V", "W", "v@x.com", VENDEDOR);
        assertTrue(v.tiene(PROPIEDADES_EDITAR));
        assertFalse(v.tiene(PROPIEDADES_BAJA));
        assertEquals(v.permisos().stream().sorted().toList(), v.permisos()); // ordenados para el panel
    }
}

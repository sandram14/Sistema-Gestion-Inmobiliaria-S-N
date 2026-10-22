package py.sn.inmobiliaria.mapper;

import py.sn.inmobiliaria.model.Barrio;
import py.sn.inmobiliaria.model.Casa;
import py.sn.inmobiliaria.model.Ciudad;
import py.sn.inmobiliaria.model.Departamento;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.OperacionPropiedad;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Terreno;
import py.sn.inmobiliaria.model.TipoPropiedad;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Construye entidades en memoria para probar los mappers sin base de datos.
 * Usa reflexión porque las entidades de catálogo no tienen setters (son de solo lectura).
 */
final class Entidades {

    private Entidades() {
    }

    static <T> T crear(Class<T> clase, Map<String, Object> campos) {
        try {
            Constructor<T> c = clase.getDeclaredConstructor();
            c.setAccessible(true);
            T instancia = c.newInstance();
            campos.forEach((nombre, valor) -> asignar(instancia, nombre, valor));
            return instancia;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void asignar(Object objeto, String campo, Object valor) {
        try {
            Field f = objeto.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static final Departamento ALTO_PARANA = crear(Departamento.class, Map.of("id", 1, "nombre", "ALTO PARANÁ", "codigo", "10"));
    static final Ciudad CDE = crear(Ciudad.class, Map.of("id", 1, "nombre", "CIUDAD DEL ESTE", "codigoDistrito", "01", "departamento", ALTO_PARANA));
    static final Barrio AREA_1 = crear(Barrio.class, Map.of("id", 10011013, "nombre", "AREA 1", "ciudad", CDE));
    static final TipoPropiedad CASA = crear(TipoPropiedad.class, Map.of("id", 1, "nombre", "Casa"));
    static final TipoPropiedad TERRENO = crear(TipoPropiedad.class, Map.of("id", 2, "nombre", "Terreno"));
    static final EstadoPropiedad DISPONIBLE = crear(EstadoPropiedad.class, Map.of("id", 1, "nombre", "Disponible"));
    static final Operacion VENTA = crear(Operacion.class, Map.of("id", 1, "nombre", "Venta"));
    static final Operacion ALQUILER = crear(Operacion.class, Map.of("id", 2, "nombre", "Alquiler"));

    /** Equivalente a prop-001 de la carga inicial, con un alquiler agregado. */
    static Propiedad casaProp001() {
        Propiedad p = new Propiedad("prop-001", "Casa en Area 1", new BigDecimal("-25.511700"),
                new BigDecimal("-54.611800"), CASA, DISPONIBLE, AREA_1);
        asignar(p, "id", 1L);
        p.setDireccion("Calle Curupayty 519");
        Casa casa = new Casa((short) 3, (short) 2, new BigDecimal("182.00"));
        casa.setCocina(true);
        p.asignarCasa(casa);
        // Se agrega primero el alquiler para comprobar que el mapper ordena Venta primero
        p.agregarOperacion(new OperacionPropiedad(ALQUILER, new BigDecimal("4000000.00"), Moneda.PYG));
        p.agregarOperacion(new OperacionPropiedad(VENTA, new BigDecimal("45000.00"), Moneda.PYG));
        for (short i = 0; i < 3; i++) {
            Imagen img = new Imagen("/images/propiedades/prop-001/" + (i + 1) + ".png", i == 0, i);
            asignar(img, "id", 100L + i);
            p.agregarImagen(img);
        }
        return p;
    }

    static Propiedad terrenoSinDireccion() {
        Propiedad p = new Propiedad("terr-001", "Terreno", new BigDecimal("-25.5"), new BigDecimal("-54.6"),
                TERRENO, DISPONIBLE, AREA_1);
        asignar(p, "id", 2L);
        p.asignarTerreno(new Terreno(new BigDecimal("750.00")));
        p.agregarOperacion(new OperacionPropiedad(VENTA, new BigDecimal("41000.00"), Moneda.PYG));
        return p;
    }
}

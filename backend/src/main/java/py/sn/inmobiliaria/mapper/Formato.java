package py.sn.inmobiliaria.mapper;

import py.sn.inmobiliaria.dto.response.geojson.PropiedadProps.OperacionDto;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.OperacionPropiedad;
import py.sn.inmobiliaria.model.Propiedad;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Utilidades de formato compartidas por los mappers. */
final class Formato {

    /** Paleta verde del mapa (los mismos tonos que tenían los GeoJSON originales). */
    private static final String[] PALETA = {
            "#2E5438", "#345A3E", "#3A6B46", "#3D6050", "#4A7A56", "#4F7A57", "#526B3A"};

    private Formato() {
    }

    /** Color estable para un id: el mismo barrio siempre tiene el mismo color. */
    static String color(long id) {
        return PALETA[(int) Math.floorMod(id, (long) PALETA.length)];
    }

    /** 45000.00 → 45000; 95.50 → 95.5 (sin ceros sobrantes). */
    static BigDecimal numero(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
    }

    /** Operaciones de la propiedad: Venta primero, luego Alquiler. */
    static List<OperacionDto> operaciones(Propiedad p) {
        return p.getOperaciones().stream()
                .sorted(Comparator.comparing((OperacionPropiedad op) -> !Operacion.VENTA.equals(op.getOperacion().getNombre())))
                .map(op -> new OperacionDto(op.getOperacion().getNombre(), numero(op.getPrecio()), op.getMoneda().name()))
                .toList();
    }

    static String textoONulo(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}

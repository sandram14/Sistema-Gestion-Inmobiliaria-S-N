package py.sn.inmobiliaria.service.validacion;

import py.sn.inmobiliaria.model.Moneda;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de una propiedad ya validados y normalizados (textos recortados, catálogos con
 * su nombre oficial, decimales redondeados a la precisión de la base).
 *
 * @param estado nombre del estado, o null si no se indicó (al editar: se conserva el actual)
 * @param casa   datos de casa si tipo = Casa; null si es Terreno
 * @param terreno datos de terreno si tipo = Terreno; null si es Casa
 */
public record PropiedadValidada(
        String titulo,
        String descripcion,
        String direccion,
        BigDecimal latitud,
        BigDecimal longitud,
        int idBarrio,
        String tipo,
        String estado,
        CasaValidada casa,
        TerrenoValidado terreno,
        List<OperacionValidada> operaciones) {

    public record CasaValidada(short habitaciones, short banos, boolean sala, boolean comedor, boolean cocina,
                               BigDecimal superficieConstruida, BigDecimal superficieTerreno) {
    }

    public record TerrenoValidado(BigDecimal superficie, BigDecimal frente, BigDecimal fondo, boolean tieneTitulo) {
    }

    public record OperacionValidada(String operacion, BigDecimal precio, Moneda moneda) {
    }
}

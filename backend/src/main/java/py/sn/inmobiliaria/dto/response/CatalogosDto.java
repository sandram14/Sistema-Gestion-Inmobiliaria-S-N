package py.sn.inmobiliaria.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Respuesta de GET /api/catalogos: todo lo que necesitan los formularios del panel.
 *
 * @param operacionesPorTipo {"Casa": ["Venta","Alquiler"], "Terreno": ["Venta"]}: el formulario
 *                           puede ocultar las combinaciones que el backend rechazaría
 * @param departamentos      árbol Departamento → Ciudad → Barrio (sin geometrías)
 */
public record CatalogosDto(
        List<String> tipos,
        List<String> estados,
        List<String> operaciones,
        List<String> monedas,
        Map<String, List<String>> operacionesPorTipo,
        List<DepartamentoDto> departamentos) {

    public record DepartamentoDto(int id, String nombre, List<CiudadDto> ciudades) {
    }

    public record CiudadDto(int id, String nombre, List<BarrioDto> barrios) {
    }

    public record BarrioDto(int id, String nombre) {
    }
}

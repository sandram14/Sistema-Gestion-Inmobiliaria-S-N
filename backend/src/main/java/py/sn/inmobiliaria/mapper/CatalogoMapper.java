package py.sn.inmobiliaria.mapper;

import py.sn.inmobiliaria.dto.response.CatalogosDto;
import py.sn.inmobiliaria.dto.response.CatalogosDto.BarrioDto;
import py.sn.inmobiliaria.dto.response.CatalogosDto.CiudadDto;
import py.sn.inmobiliaria.dto.response.CatalogosDto.DepartamentoDto;
import py.sn.inmobiliaria.dto.response.admin.UbicacionDetectadaDto;
import py.sn.inmobiliaria.dto.response.admin.UbicacionDetectadaDto.Elemento;
import py.sn.inmobiliaria.repository.UbicacionDao.FilaUbicacion;
import py.sn.inmobiliaria.service.CatalogoService.Catalogos;
import py.sn.inmobiliaria.service.UbicacionService.UbicacionDetectada;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte los datos de catálogo y ubicación: GET /api/catalogos (jerarquía como árbol)
 * y GET /api/admin/ubicacion (la jerarquía de un punto).
 */
public final class CatalogoMapper {

    private CatalogoMapper() {
    }

    public static CatalogosDto dto(Catalogos c) {
        return new CatalogosDto(c.tipos(), c.estados(), c.operaciones(), c.monedas(), c.operacionesPorTipo(),
                arbol(c.ubicaciones()));
    }

    /** Misma forma de departamento/ciudad/barrio que el árbol de catálogos (id + nombre). */
    public static UbicacionDetectadaDto ubicacion(UbicacionDetectada u) {
        FilaUbicacion f = u.ubicacion();
        return new UbicacionDetectadaDto(u.latitud(), u.longitud(),
                new Elemento(f.idDepartamento(), f.departamento()),
                new Elemento(f.idCiudad(), f.ciudad()),
                new Elemento(f.idBarrio(), f.barrio()));
    }

    /**
     * Filas planas (un barrio por fila, ya ordenadas) → árbol departamento → ciudad → barrio.
     * Se respeta el orden de llegada.
     */
    static List<DepartamentoDto> arbol(List<FilaUbicacion> filas) {
        Map<Integer, String> departamentos = new LinkedHashMap<>();
        Map<Integer, Map<Integer, String>> ciudadesPorDepartamento = new LinkedHashMap<>();
        Map<Integer, List<BarrioDto>> barriosPorCiudad = new LinkedHashMap<>();

        for (FilaUbicacion f : filas) {
            departamentos.putIfAbsent(f.idDepartamento(), f.departamento());
            ciudadesPorDepartamento.computeIfAbsent(f.idDepartamento(), k -> new LinkedHashMap<>())
                    .putIfAbsent(f.idCiudad(), f.ciudad());
            barriosPorCiudad.computeIfAbsent(f.idCiudad(), k -> new ArrayList<>())
                    .add(new BarrioDto(f.idBarrio(), f.barrio()));
        }

        List<DepartamentoDto> resultado = new ArrayList<>();
        departamentos.forEach((idDep, nombreDep) -> {
            List<CiudadDto> ciudades = new ArrayList<>();
            ciudadesPorDepartamento.get(idDep).forEach((idCiudad, nombreCiudad) ->
                    ciudades.add(new CiudadDto(idCiudad, nombreCiudad, List.copyOf(barriosPorCiudad.get(idCiudad)))));
            resultado.add(new DepartamentoDto(idDep, nombreDep, List.copyOf(ciudades)));
        });
        return List.copyOf(resultado);
    }
}

package py.sn.inmobiliaria.mapper;

import org.junit.jupiter.api.Test;
import py.sn.inmobiliaria.dto.response.CatalogosDto.DepartamentoDto;
import py.sn.inmobiliaria.repository.UbicacionDao.FilaUbicacion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogoMapperTest {

    @Test
    void convierteFilasPlanasEnArbol() {
        List<DepartamentoDto> arbol = CatalogoMapper.arbol(List.of(
                new FilaUbicacion(1, "ALTO PARANÁ", 1, "CIUDAD DEL ESTE", 10011013, "AREA 1"),
                new FilaUbicacion(1, "ALTO PARANÁ", 1, "CIUDAD DEL ESTE", 10011024, "CIUDAD NUEVA"),
                new FilaUbicacion(1, "ALTO PARANÁ", 3, "HERNANDARIAS", 10051007, "NUESTRA SRA DE LA ASUNCION")));

        assertEquals(1, arbol.size());
        assertEquals("ALTO PARANÁ", arbol.get(0).nombre());
        assertEquals(2, arbol.get(0).ciudades().size());
        assertEquals("CIUDAD DEL ESTE", arbol.get(0).ciudades().get(0).nombre());
        assertEquals(2, arbol.get(0).ciudades().get(0).barrios().size());
        assertEquals(10051007, arbol.get(0).ciudades().get(1).barrios().get(0).id());
    }

    @Test
    void sinFilasDevuelveListaVacia() {
        assertEquals(0, CatalogoMapper.arbol(List.of()).size());
    }
}

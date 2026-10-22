package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.model.EstadoPropiedad;
import py.sn.inmobiliaria.model.Moneda;
import py.sn.inmobiliaria.model.Operacion;
import py.sn.inmobiliaria.model.TipoPropiedad;
import py.sn.inmobiliaria.repository.CatalogoDao;
import py.sn.inmobiliaria.repository.UbicacionDao.FilaUbicacion;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Valores para los formularios del panel: catálogos, reglas y jerarquía geográfica. */
public class CatalogoService extends ServicioBase {

    private final CatalogoDao catalogoDao;
    private final UbicacionService ubicacionService;

    public CatalogoService(TxRunner tx, CatalogoDao catalogoDao, UbicacionService ubicacionService) {
        super(tx);
        this.catalogoDao = catalogoDao;
        this.ubicacionService = ubicacionService;
    }

    public Catalogos obtener() {
        Catalogos sinUbicaciones = leer(em -> new Catalogos(
                catalogoDao.listarTipos(em).stream().map(TipoPropiedad::getNombre).toList(),
                catalogoDao.listarEstados(em).stream().map(EstadoPropiedad::getNombre).toList(),
                catalogoDao.listarOperaciones(em).stream().map(Operacion::getNombre).toList(),
                Arrays.stream(Moneda.values()).map(Enum::name).toList(),
                ReglasNegocio.operacionesPorTipo(),
                List.of()));
        return new Catalogos(sinUbicaciones.tipos(), sinUbicaciones.estados(), sinUbicaciones.operaciones(),
                sinUbicaciones.monedas(), sinUbicaciones.operacionesPorTipo(), ubicacionService.listarJerarquia());
    }

    /**
     * @param operacionesPorTipo regla de negocio para que el formulario no ofrezca combinaciones inválidas
     * @param ubicaciones        departamento → ciudad → barrio (sin geometrías)
     */
    public record Catalogos(
            List<String> tipos,
            List<String> estados,
            List<String> operaciones,
            List<String> monedas,
            Map<String, List<String>> operacionesPorTipo,
            List<FilaUbicacion> ubicaciones) {
    }
}

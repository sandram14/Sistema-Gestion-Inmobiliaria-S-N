package py.sn.inmobiliaria.service;

import py.sn.inmobiliaria.config.TxRunner;
import py.sn.inmobiliaria.dto.request.BusquedaRequest;
import py.sn.inmobiliaria.exception.NoEncontradoException;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.repository.FiltroPropiedades;
import py.sn.inmobiliaria.repository.PropiedadDao;
import py.sn.inmobiliaria.service.validacion.NormalizadorBusqueda;

import java.util.List;
import java.util.function.Function;

/**
 * Consultas de la parte pública: solo propiedades Disponibles.
 *
 * <p>Los métodos reciben un "conversor" ({@code Function<Propiedad, T>}) que se aplica
 * dentro de la transacción. Así el service decide qué se lee y con qué reglas, y quien
 * llama decide la forma de la respuesta (en la Fase 8: GeoJSON para el mapa). Es como
 * pedir un plato indicando cómo servirlo: la cocina sigue siendo la misma.</p>
 */
public class PropiedadConsultaService extends ServicioBase {

    private final PropiedadDao propiedadDao;

    public PropiedadConsultaService(TxRunner tx, PropiedadDao propiedadDao) {
        super(tx);
        this.propiedadDao = propiedadDao;
    }

    /**
     * @throws py.sn.inmobiliaria.exception.ValidacionException si los criterios son inválidos
     *                                                            (por ejemplo, Terreno + Alquiler)
     */
    public <T> List<T> buscar(BusquedaRequest criterios, Function<Propiedad, T> conversor) {
        FiltroPropiedades filtro = NormalizadorBusqueda.normalizar(criterios);
        return leer(em -> propiedadDao.buscarDisponibles(em, filtro).stream().map(conversor).toList());
    }

    /**
     * @throws NoEncontradoException si no existe o no está disponible
     */
    public <T> T obtenerPorCodigo(String codigo, Function<Propiedad, T> conversor) {
        String c = codigo == null ? "" : codigo.trim().toLowerCase();
        return leer(em -> propiedadDao.buscarDisponiblePorCodigo(em, c)
                .map(conversor)
                .orElseThrow(() -> new NoEncontradoException("La propiedad no existe o ya no está disponible")));
    }
}

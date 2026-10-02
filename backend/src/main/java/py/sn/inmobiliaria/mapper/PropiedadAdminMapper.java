package py.sn.inmobiliaria.mapper;

import py.sn.inmobiliaria.dto.response.admin.PropiedadDetalleDto;
import py.sn.inmobiliaria.dto.response.admin.PropiedadResumenDto;
import py.sn.inmobiliaria.model.Barrio;
import py.sn.inmobiliaria.model.Casa;
import py.sn.inmobiliaria.model.Ciudad;
import py.sn.inmobiliaria.model.Imagen;
import py.sn.inmobiliaria.model.Propiedad;
import py.sn.inmobiliaria.model.Terreno;

/** DTOs del panel administrativo: listado y formulario de edición. */
public final class PropiedadAdminMapper {

    private PropiedadAdminMapper() {
    }

    public static PropiedadResumenDto resumen(Propiedad p) {
        String imagenPrincipal = p.getImagenes().stream()
                .filter(Imagen::isEsPrincipal).map(Imagen::getUrl).findFirst().orElse(null);
        return new PropiedadResumenDto(
                p.getId(),
                p.getCodigo(),
                p.getTitulo(),
                p.getTipo().getNombre(),
                p.getEstado().getNombre(),
                p.getBarrio().getCiudad().getNombre(),
                p.getBarrio().getNombre(),
                Formato.operaciones(p),
                imagenPrincipal,
                p.getActualizadoEn());
    }

    public static PropiedadDetalleDto detalle(Propiedad p) {
        Barrio barrio = p.getBarrio();
        Ciudad ciudad = barrio.getCiudad();
        Casa c = p.getCasa();
        Terreno t = p.getTerreno();
        return new PropiedadDetalleDto(
                p.getId(),
                p.getCodigo(),
                p.getTitulo(),
                p.getDescripcion(),
                p.getDireccion(),
                p.getLatitud(),
                p.getLongitud(),
                p.getTipo().getNombre(),
                p.getEstado().getNombre(),
                new PropiedadDetalleDto.Ubicacion(barrio.getId(), barrio.getNombre(), ciudad.getId(), ciudad.getNombre(),
                        ciudad.getDepartamento().getId(), ciudad.getDepartamento().getNombre()),
                c == null ? null : new PropiedadDetalleDto.Casa(c.getHabitaciones(), c.getBanos(), c.isSala(),
                        c.isComedor(), c.isCocina(), Formato.numero(c.getSuperficieConstruida()),
                        Formato.numero(c.getSuperficieTerreno())),
                t == null ? null : new PropiedadDetalleDto.Terreno(Formato.numero(t.getSuperficie()),
                        Formato.numero(t.getFrente()), Formato.numero(t.getFondo()), t.isTieneTitulo()),
                Formato.operaciones(p),
                p.getImagenes().stream()
                        .map(i -> new PropiedadDetalleDto.ImagenDto(i.getId(), i.getUrl(), i.getDescripcion(),
                                i.isEsPrincipal(), i.getOrden()))
                        .toList(),
                p.getCreadoEn(),
                p.getActualizadoEn());
    }
}

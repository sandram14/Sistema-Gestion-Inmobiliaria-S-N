import { useEffect, useState } from "react";
import { apiGet } from "../../services/api";
import type {
  CiudadFeature,
  BarrioFeature,
  PropiedadFeature,
  GeoFeatureCollection,
} from "./types";

interface GeoDataState {
  ciudades: CiudadFeature[];
  barrios: BarrioFeature[];
  propiedades: PropiedadFeature[];
  loading: boolean;
  error: string | null;
}

/**
 * Deja solo las zonas donde hay propiedades disponibles: los barrios con al menos una propiedad y las ciudades
 * con al menos uno de esos barrios (con su cantidad de barrios recalculada). Se calcula con la lista de
 * propiedades, que el backend nunca deja guardar en el navegador: un barrio aparece o desaparece apenas se
 * carga, se vende o se da de baja una propiedad. Los contornos sí se guardan unos minutos (cargan rápido).
 */
export function soloConPropiedades(
  ciudades: CiudadFeature[],
  barrios: BarrioFeature[],
  propiedades: PropiedadFeature[]
): { ciudades: CiudadFeature[]; barrios: BarrioFeature[] } {
  const conPropiedades = new Set(propiedades.map((p) => p.properties.barrioId));
  const barriosConPropiedades = barrios.filter((b) => conPropiedades.has(b.properties.id));
  const porCiudad = new Map<string, number>();
  for (const b of barriosConPropiedades) {
    porCiudad.set(b.properties.distritoCod, (porCiudad.get(b.properties.distritoCod) ?? 0) + 1);
  }
  return {
    ciudades: ciudades
      .filter((c) => porCiudad.has(c.properties.id))
      .map((c) => ({ ...c, properties: { ...c.properties, totalBarrios: porCiudad.get(c.properties.id)! } })),
    barrios: barriosConPropiedades,
  };
}

/**
 * Carga ciudades, barrios y propiedades desde la API Java:
 * /api/ciudades, /api/barrios y /api/propiedades (mismo formato FeatureCollection
 * que tenían los archivos estáticos de public/data/, que ya no se usan).
 *
 * "propiedades" trae todas las disponibles (para Ofertas y para abrir el detalle
 * desde un enlace); la búsqueda con filtros la hace el buscador contra la API.
 */
export function useGeoData(apiBaseUrl: string = "/api"): GeoDataState {
  const [state, setState] = useState<GeoDataState>({
    ciudades: [],
    barrios: [],
    propiedades: [],
    loading: true,
    error: null,
  });

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        // Solo las ciudades con propiedades publicadas y sus barrios (con los 714 barrios del INE cargados,
        // todo Alto Paraná pesaría mucho más y llenaría el mapa de ciudades vacías)
        const [ciudadesFC, barriosFC, propiedadesFC] = await Promise.all([
          apiGet<GeoFeatureCollection<CiudadFeature>>(`${apiBaseUrl}/ciudades?conPropiedades=true`),
          apiGet<GeoFeatureCollection<BarrioFeature>>(`${apiBaseUrl}/barrios?conPropiedades=true`),
          apiGet<GeoFeatureCollection<PropiedadFeature>>(`${apiBaseUrl}/propiedades`),
        ]);
        if (!cancelled) {
          // En el mapa y en las listas del buscador, solo donde hay propiedades
          const zonas = soloConPropiedades(ciudadesFC.features, barriosFC.features, propiedadesFC.features);
          setState({
            ciudades: zonas.ciudades,
            barrios: zonas.barrios,
            propiedades: propiedadesFC.features,
            loading: false,
            error: null,
          });
        }
      } catch (err) {
        if (!cancelled) {
          setState((s) => ({
            ...s,
            loading: false,
            error: err instanceof Error ? err.message : "Error desconocido cargando el mapa.",
          }));
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [apiBaseUrl]);

  return state;
}

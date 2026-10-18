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
          setState({
            ciudades: ciudadesFC.features,
            barrios: barriosFC.features,
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

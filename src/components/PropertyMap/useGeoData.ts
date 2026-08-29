import { useEffect, useState } from "react";
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
 * Carga ciudades.geojson / barrios.geojson / propiedades.geojson desde
 * `public/data/`. Cuando tengas un backend real de listados, reemplazá
 * el fetch de `propiedades` por tu endpoint (mismo shape de FeatureCollection).
 */
export function useGeoData(dataBaseUrl: string = "/data"): GeoDataState {
  const [state, setState] = useState<GeoDataState>({
    ciudades: [],
    barrios: [],
    propiedades: [],
    loading: true,
    error: null,
  });

  useEffect(() => {
    let cancelled = false;

    async function fetchJson<T>(path: string): Promise<T> {
      const res = await fetch(path);
      if (!res.ok) throw new Error(`No se pudo cargar ${path} (HTTP ${res.status})`);
      return res.json() as Promise<T>;
    }

    async function load() {
      try {
        const [ciudadesFC, barriosFC, propiedadesFC] = await Promise.all([
          fetchJson<GeoFeatureCollection<CiudadFeature>>(`${dataBaseUrl}/ciudades.geojson`),
          fetchJson<GeoFeatureCollection<BarrioFeature>>(`${dataBaseUrl}/barrios.geojson`),
          fetchJson<GeoFeatureCollection<PropiedadFeature>>(`${dataBaseUrl}/propiedades.geojson`),
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
  }, [dataBaseUrl]);

  return state;
}

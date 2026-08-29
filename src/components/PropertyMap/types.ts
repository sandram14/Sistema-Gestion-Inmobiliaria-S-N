// Tipos compartidos por SearchFilter.tsx y MapSection (App.tsx).
// El día que haya un backend real de propiedades, sólo cambia de dónde
// viene `PropiedadFeature[]` (ver useGeoData.ts) — el resto queda igual.

// Categoría real del dato (así viene en el geojson / futura API).
export type CategoriaPropiedad = "Casa" | "Departamento" | "Terreno";
export type TipoOperacion = "Venta" | "Alquiler";

// Agrupación que usa la UI: Casa + Departamento se muestran juntos como
// "Vivienda" (comprable o alquilable). Terreno es su propio grupo y sólo
// puede comprarse. Los íconos de casa/depto en el mapa se mantienen
// distintos aunque compartan el mismo filtro/grupo.
export type GrupoPropiedad = "Vivienda" | "Terreno";

export function grupoDeCategoria(categoria: CategoriaPropiedad): GrupoPropiedad {
  return categoria === "Terreno" ? "Terreno" : "Vivienda";
}

export interface PointGeometry {
  type: "Point";
  coordinates: [number, number]; // [lng, lat]
}
export interface PolygonalGeometry {
  type: "Polygon" | "MultiPolygon";
  coordinates: unknown;
}

export interface GeoFeature<G, P> {
  type: "Feature";
  geometry: G;
  properties: P;
}
export interface GeoFeatureCollection<F> {
  type: "FeatureCollection";
  features: F[];
}

export interface CiudadProps {
  id: string; // código de distrito
  ciudad: string;
  departamento: string;
  totalBarrios: number;
  color: string;
}

export interface BarrioProps {
  id: number; // CLAVE_BAR
  barrio: string;
  distritoCod: string;
  ciudad: string;
  departamento: string;
  color: string;
}

export interface PropiedadProps {
  id: string;
  titulo: string;
  categoria: CategoriaPropiedad;
  tipoOperacion: TipoOperacion;
  ciudad: string;
  barrio: string;
  barrioId: number;
  precio: number;
  moneda: string;
  dormitorios: number;
  banos: number;
  m2: number;
  direccion: string;
  /** Rutas a las fotos reales (mínimo 3), ej: "/images/propiedades/prop-001/1.png". */
  fotos?: string[];
}

export type CiudadFeature = GeoFeature<PointGeometry, CiudadProps>;
export type BarrioFeature = GeoFeature<PolygonalGeometry, BarrioProps>;
export type PropiedadFeature = GeoFeature<PointGeometry, PropiedadProps>;

export type Nivel = "departamento" | "ciudad" | "barrio";

export interface Filtros {
  ciudadId: string | null;
  barrioId: number | null;
  categoria: GrupoPropiedad | "Todos";
  tipoOperacion: TipoOperacion | "Todos";
  presupuestoMax: number | null;
}

export const FILTROS_VACIOS: Filtros = {
  ciudadId: null,
  barrioId: null,
  categoria: "Todos",
  tipoOperacion: "Todos",
  presupuestoMax: null,
};
  
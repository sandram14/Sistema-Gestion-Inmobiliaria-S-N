// Tipos compartidos por SearchFilter.tsx y MapSection (App.tsx).
// Los datos vienen de la API Java (ver useGeoData.ts) con el mismo formato GeoJSON
// que tenían los archivos estáticos; los campos nuevos de la API son opcionales.

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
  id: string; // identificador de la ciudad
  ciudad: string;
  departamento: string;
  totalBarrios: number;
  color: string;
  /** Campo de la API: id del departamento. */
  departamentoId?: number;
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
  /** Campo de la API: todas las operaciones con su precio (una casa puede tener Venta y Alquiler). */
  operaciones?: OperacionPrecio[];
  /** Campo de la API: descripción cargada desde el panel. */
  descripcion?: string | null;
}

export interface OperacionPrecio {
  operacion: TipoOperacion;
  precio: number;
  moneda: string;
}

/** Operaciones de una propiedad; si la API no envía "operaciones", usa la principal. */
export function operacionesDe(props: PropiedadProps): OperacionPrecio[] {
  if (props.operaciones && props.operaciones.length > 0) return props.operaciones;
  return [{ operacion: props.tipoOperacion, precio: props.precio, moneda: props.moneda }];
}

export function tieneOperacion(props: PropiedadProps, operacion: TipoOperacion): boolean {
  return operacionesDe(props).some((o) => o.operacion === operacion);
}

export type CiudadFeature = GeoFeature<PointGeometry, CiudadProps>;
export type BarrioFeature = GeoFeature<PolygonalGeometry, BarrioProps>;
export type PropiedadFeature = GeoFeature<PointGeometry, PropiedadProps>;

export type Nivel = "departamento" | "ciudad" | "barrio";

export interface Filtros {
  departamentoId: number | null;
  ciudadId: string | null;
  barrioId: number | null;
  categoria: GrupoPropiedad | "Todos";
  tipoOperacion: TipoOperacion | "Todos";
  presupuestoMax: number | null;
}

export const FILTROS_VACIOS: Filtros = {
  departamentoId: null,
  ciudadId: null,
  barrioId: null,
  categoria: "Todos",
  tipoOperacion: "Todos",
  presupuestoMax: null,
};
  
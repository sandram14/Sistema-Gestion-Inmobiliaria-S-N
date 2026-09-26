import L from "leaflet";
import type { BarrioFeature, CategoriaPropiedad } from "./types";

export function boundsFromFeatures(
  features: Array<{ type: "Feature"; geometry: any; properties: unknown }>
): L.LatLngBounds | null {
  if (features.length === 0) return null;
  const layer = L.geoJSON(features as any);
  const bounds = layer.getBounds();
  return bounds.isValid() ? bounds : null;
}

export function boundsForCiudad(barrios: BarrioFeature[], ciudadId: string): L.LatLngBounds | null {
  return boundsFromFeatures(barrios.filter((b) => b.properties.distritoCod === ciudadId));
}

export function boundsForBarrio(barrios: BarrioFeature[], barrioId: number): L.LatLngBounds | null {
  const feature = barrios.find((b) => b.properties.id === barrioId);
  return feature ? boundsFromFeatures([feature]) : null;
}

/** Bounds acotados alrededor de un punto exacto (para la vista de "ubicación exacta"). */
export function boundsForPoint(lat: number, lng: number, metros = 350): L.LatLngBounds {
  return L.latLng(lat, lng).toBounds(metros);
}

/** Ícono de ciudad: círculo lima con borde oscuro, en línea con la marca. */
export function ciudadIcon(): L.DivIcon {
  return L.divIcon({
    className: "pm-ciudad-icon-wrapper",
    html: `<span class="pm-ciudad-icon"></span>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  });
}

/**
 * Ícono de propiedad: pin con símbolo según categoría.
 * Casa y Departamento son "Vivienda" (comparten grupo/filtro) pero
 * conservan cada una su propio símbolo. Terreno usa un ícono de roca,
 * siempre dentro de la paleta verde de la marca.
 */
export function propiedadIcon(categoria: CategoriaPropiedad): L.DivIcon {
  const symbol =
    categoria === "Casa"
      ? `<path d="M3 10.5 12 3l9 7.5" /><path d="M5 9.5V20h14V9.5" /><path d="M9.5 20v-6h5v6" />`
      : categoria === "Departamento"
        ? `<rect x="6" y="3" width="12" height="18" rx="1" /><path d="M9 7h1M14 7h1M9 11h1M14 11h1M9 15h1M14 15h1" />`
        : // Terreno: roca/piedra
          `<path d="M3 17.3c-.6-1.9.5-3.9 2.4-4.5.2-2.4 2.2-4.3 4.6-4.3 2.1 0 3.9 1.4 4.4 3.4 2.1.1 3.8 1.9 3.8 4a4 4 0 0 1-4 4H7.5c-1.9 0-3.6-.9-4.5-2.6Z" /><path d="M8.5 13.5 9.5 15.5" /><path d="M13 12.7l.8 2.3" />`;

  const pinClass =
    categoria === "Terreno" ? "pm-propiedad-icon pm-propiedad-icon--terreno" : "pm-propiedad-icon";

  return L.divIcon({
    className: "pm-propiedad-icon-wrapper",
    html: `
      <span class="${pinClass}">
        <svg viewBox="0 0 24 24" fill="none" stroke="#23412A" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
          ${symbol}
        </svg>
      </span>`,
    iconSize: [26, 26],
    iconAnchor: [13, 26],
  });
}

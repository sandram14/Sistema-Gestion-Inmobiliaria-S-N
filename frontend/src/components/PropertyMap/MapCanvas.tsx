import { useMemo, useEffect } from "react";
import { MapContainer, TileLayer, GeoJSON, Marker, Tooltip, useMap, ZoomControl } from 'react-leaflet';
import type { LatLngBounds, StyleFunction, PathOptions, Path } from "leaflet";
import "leaflet/dist/leaflet.css";
import type { BarrioFeature, CiudadFeature, PropiedadFeature } from "./types";
import { ciudadIcon, propiedadIcon } from "./mapUtils";
import { textoPrecioOperacion } from "../../services/guaranies";
import "./PropertyMap.css";

const DEFAULT_CENTER: [number, number] = [-25.5, -54.9];
const DEFAULT_ZOOM = 10;

interface MapCanvasProps {
  ciudades: CiudadFeature[];
  barriosVisibles: BarrioFeature[];
  propiedadesVisibles: PropiedadFeature[];
  showCiudades: boolean;
  showBarrios: boolean;
  showPropiedades: boolean;
  barriosLayerKey: string;
  selectedBarrioId: number | null;
  flyToBounds: LatLngBounds | null;
  onSelectCiudad: (id: string) => void;
  onSelectBarrio: (id: number, nombre: string) => void;
  onSelectPropiedad?: (id: string) => void;
  heightClassName?: string;
}

function FlyToBounds({ bounds }: { bounds: LatLngBounds | null }) {
  const map = useMap();
  useEffect(() => {
    if (bounds && bounds.isValid()) {
      map.flyToBounds(bounds, { padding: [48, 48], duration: 0.85 });
    }
  }, [bounds, map]);
  return null;
}

function barrioStyle(feature: BarrioFeature, selectedBarrioId: number | null): PathOptions {
  const isSelected = feature.properties.id === selectedBarrioId;
  return {
    color: isSelected ? "#BDE688" : "rgba(168,214,111,0.35)",
    weight: isSelected ? 2.5 : 1,
    fillColor: isSelected ? "#A8D66F" : feature.properties.color,
    fillOpacity: isSelected ? 0.9 : 0.65,
  };
}

export default function MapCanvas({
  ciudades,
  barriosVisibles,
  propiedadesVisibles,
  showCiudades,
  showBarrios,
  showPropiedades,
  barriosLayerKey,
  selectedBarrioId,
  flyToBounds,
  onSelectCiudad,
  onSelectBarrio,
  onSelectPropiedad,
}: MapCanvasProps) {
  const barriosFC = useMemo(
    () => ({ type: "FeatureCollection" as const, features: barriosVisibles }),
    [barriosVisibles]
  );

  const styleFn: StyleFunction<any> = (feature) => barrioStyle(feature as BarrioFeature, selectedBarrioId);

  return (
    <MapContainer center={DEFAULT_CENTER} zoom={DEFAULT_ZOOM} scrollWheelZoom zoomControl={false} className="pm-map">
      {/* Zoom a la derecha: la esquina izquierda la usan el botón Volver y las migas de pan */}
      <ZoomControl position="topright" />
      <TileLayer
  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      />

      <FlyToBounds bounds={flyToBounds} />

      {showCiudades &&
        ciudades.map((c) => (
          <Marker
            key={c.properties.id}
            position={[c.geometry.coordinates[1], c.geometry.coordinates[0]]}
            icon={ciudadIcon()}
            eventHandlers={{ click: () => onSelectCiudad(c.properties.id) }}
          >
            <Tooltip direction="top" offset={[0, -8]} className="pm-tooltip">
              {c.properties.ciudad} · {c.properties.totalBarrios} barrio(s)
            </Tooltip>
          </Marker>
        ))}

      {showBarrios && barriosVisibles.length > 0 && (
        <GeoJSON
          key={barriosLayerKey}
          data={barriosFC as any}
          style={styleFn}
          onEachFeature={(feature, layer) => {
            const props = (feature as unknown as BarrioFeature).properties;
            layer.bindTooltip(props.barrio, { sticky: true, className: "pm-tooltip" });
            layer.on({
              click: () => onSelectBarrio(props.id, props.barrio),
              mouseover: (e) => (e.target as Path).setStyle({ weight: 2.5, fillOpacity: 0.85 }),
              mouseout: (e) =>
                (e.target as Path).setStyle(
                  barrioStyle(feature as unknown as BarrioFeature, selectedBarrioId)
                ),
            });
          }}
        />
      )}

      {showPropiedades &&
        propiedadesVisibles.map((p) => (
          <Marker
            key={p.properties.id}
            position={[p.geometry.coordinates[1], p.geometry.coordinates[0]]}
            icon={propiedadIcon(p.properties.categoria)}
            eventHandlers={{ click: () => onSelectPropiedad?.(p.properties.id) }}
          >
            <Tooltip direction="top" offset={[0, -22]} className="pm-tooltip">
              {p.properties.titulo} · {textoPrecioOperacion(p.properties.tipoOperacion, p.properties.precio)}
            </Tooltip>
          </Marker>
        ))}
    </MapContainer>
  );
}

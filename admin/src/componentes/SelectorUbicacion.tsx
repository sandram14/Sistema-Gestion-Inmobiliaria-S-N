import { useEffect, useState } from 'react'
import L from 'leaflet'
import { GeoJSON, MapContainer, Marker, TileLayer, useMapEvents } from 'react-leaflet'
import type { GeoJsonObject } from 'geojson'
import iconoUrl from 'leaflet/dist/images/marker-icon.png'
import iconoRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import sombraUrl from 'leaflet/dist/images/marker-shadow.png'
import { api, ApiError } from '../api/cliente'
import type { BarriosGeoJson, UbicacionDetectada } from '../api/tipos'
import ui from '../ui.module.css'

// Con Vite, el ícono por defecto de Leaflet necesita rutas explícitas
const icono = L.icon({
  iconUrl: iconoUrl, iconRetinaUrl: iconoRetinaUrl, shadowUrl: sombraUrl,
  iconSize: [25, 41], iconAnchor: [12, 41], shadowSize: [41, 41],
})

export interface ValorUbicacion {
  latitud: number | null
  longitud: number | null
  /** Jerarquía detectada por el backend; null si el punto está fuera de las zonas o falta marcarlo */
  detectada: UbicacionDetectada | null
}

const CENTRO_ALTO_PARANA: [number, number] = [-25.5, -54.72]

/** Clics en el mapa */
function ClicEnMapa({ alHacerClic }: { alHacerClic: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => alHacerClic(e.latlng.lat, e.latlng.lng) })
  return null
}

/**
 * El administrador marca el punto (clic o arrastrando el marcador) y el BACKEND determina,
 * con PostGIS, el barrio, la ciudad y el departamento (GET /api/admin/ubicacion). Esos datos
 * no se escriben a mano. Si el punto queda fuera de las zonas registradas, se informa y el
 * formulario no permite guardar.
 */
export default function SelectorUbicacion({
  valor,
  onCambio,
  error,
}: {
  valor: ValorUbicacion
  onCambio: (v: ValorUbicacion) => void
  error?: string
}) {
  const [barrios, setBarrios] = useState<BarriosGeoJson | null>(null)
  const [detectando, setDetectando] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)

  useEffect(() => {
    api.get<BarriosGeoJson>('/api/barrios').then(setBarrios).catch(() => setBarrios(null))
  }, [])

  async function detectar(lat: number, lng: number) {
    setDetectando(true)
    setAviso(null)
    onCambio({ latitud: lat, longitud: lng, detectada: null }) // el marcador se mueve de inmediato
    try {
      const u = await api.get<UbicacionDetectada>(
        `/api/admin/ubicacion?latitud=${lat.toFixed(6)}&longitud=${lng.toFixed(6)}`
      )
      onCambio({ latitud: u.latitud, longitud: u.longitud, detectada: u })
    } catch (e) {
      if (e instanceof ApiError && e.codigo === 'FUERA_DE_ZONA') {
        setAviso(e.message)
      } else {
        setAviso(e instanceof ApiError ? e.message : 'No se pudo determinar la ubicación.')
      }
    } finally {
      setDetectando(false)
    }
  }

  const posicion: [number, number] | null =
    valor.latitud != null && valor.longitud != null ? [valor.latitud, valor.longitud] : null

  return (
    <div>
      <p className={ui.ayuda}>
        Haga clic en el mapa sobre la ubicación de la propiedad (o arrastre el marcador). El barrio, la ciudad y el
        departamento se determinan automáticamente.
      </p>
      <div className={ui.mapa} data-testid="mapa-ubicacion">
        <MapContainer center={posicion ?? CENTRO_ALTO_PARANA} zoom={posicion ? 15 : 11} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          />
          {barrios && (
            <GeoJSON
              data={barrios as unknown as GeoJsonObject}
              style={() => ({ color: '#2e5438', weight: 1, fillColor: '#4a7a56', fillOpacity: 0.15 })}
              onEachFeature={(f, capa) => {
                capa.bindTooltip(`${f.properties.barrio} (${f.properties.ciudad})`, { sticky: true })
                capa.on('click', (e: L.LeafletMouseEvent) => {
                  L.DomEvent.stopPropagation(e) // evita que el clic se procese dos veces
                  void detectar(e.latlng.lat, e.latlng.lng)
                })
              }}
            />
          )}
          <ClicEnMapa alHacerClic={(lat, lng) => void detectar(lat, lng)} />
          {posicion && (
            <Marker
              position={posicion}
              icon={icono}
              draggable
              eventHandlers={{
                dragend: (e) => {
                  const p = (e.target as L.Marker).getLatLng()
                  void detectar(p.lat, p.lng)
                },
              }}
            />
          )}
        </MapContainer>
      </div>

      {detectando && <p className={ui.ayuda}>Determinando el barrio…</p>}
      {aviso && <div className={ui.avisoError} role="alert" style={{ marginTop: '0.75rem' }}>{aviso}</div>}
      {error && !aviso && <p className={ui.errorCampo}>{error}</p>}

      <div className={ui.ubicacionDetectada} aria-live="polite">
        <div className={ui.ubicacionDato}><span>Departamento</span>{valor.detectada?.departamento.nombre ?? '—'}</div>
        <div className={ui.ubicacionDato}><span>Ciudad</span>{valor.detectada?.ciudad.nombre ?? '—'}</div>
        <div className={ui.ubicacionDato}><span>Barrio</span>{valor.detectada?.barrio.nombre ?? '—'}</div>
        <div className={ui.ubicacionDato}>
          <span>Coordenadas</span>
          {posicion ? `${posicion[0].toFixed(6)}, ${posicion[1].toFixed(6)}` : '—'}
        </div>
      </div>
    </div>
  )
}

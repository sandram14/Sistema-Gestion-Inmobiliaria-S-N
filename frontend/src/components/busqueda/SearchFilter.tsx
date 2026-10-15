import { useEffect, useMemo, useRef, useState } from 'react'
import type { LatLngBounds } from 'leaflet'
import { useSearchParams } from 'react-router-dom'
import styles from './SearchFilter.module.css'
import MapCanvas from '../PropertyMap/MapCanvas'
import { Bath, BedDouble, Building2, ChevronLeft, ChevronRight, Home, LandPlot, MapPin, MessageCircle, Ruler, X } from 'lucide-react'
import { useGeoData } from '../PropertyMap/useGeoData'
import { apiGet, esCancelacion } from '../../services/api'
import { abrirAsistenteConPropiedad } from '../chat/AssistantWidget'
import {
  boundsForBarrio,
  boundsForCiudad,
  boundsForPoint,
  boundsFromFeatures,
} from '../PropertyMap/mapUtils'
import {
  FILTROS_VACIOS,
  operacionesDe,
  tieneOperacion,
  type Filtros,
  type GeoFeatureCollection,
  type OperacionPrecio,
  type PropiedadFeature,
} from '../PropertyMap/types'

// Los presupuestos están en dólares: el backend compara solo contra precios en USD
// (no convierte monedas), por eso la búsqueda envía moneda=USD junto con el tope.
const MONEDA_PRESUPUESTO = 'USD'
const PRESUPUESTOS = [
  { label: 'Hasta USD 40.000', value: 40000 },
  { label: 'Hasta USD 70.000', value: 70000 },
  { label: 'Hasta USD 100.000', value: 100000 },
  { label: 'Hasta USD 160.000', value: 160000 },
]

export function iconoCategoria(categoria: PropiedadFeature['properties']['categoria']) {
  if (categoria === 'Casa') return <Home size={40} strokeWidth={1.5} aria-hidden="true" />
  if (categoria === 'Departamento') return <Building2 size={40} strokeWidth={1.5} aria-hidden="true" />
  return <LandPlot size={40} strokeWidth={1.5} aria-hidden="true" />
}

// ─── Fotos de las propiedades ────────────────────────────────────────────
// Cada propiedad trae su campo `fotos` desde la API (la principal primero). Los
// archivos están en el almacenamiento compartido del backend (/uploads/...) y se
// cargan o cambian desde el panel administrativo.
export function fotosPara(props: PropiedadFeature['properties']): string[] {
  return props.fotos ?? []
}

/** "USD 45.000" o "PYG 4.000.000 / mes" (alquiler). */
function textoPrecio(op: OperacionPrecio): string {
  return `${op.moneda} ${op.precio.toLocaleString()}${op.operacion === 'Alquiler' ? ' / mes' : ''}`
}

/** Búsqueda con filtros contra la API: el backend aplica y valida las reglas. */
function usePropiedadesFiltradas(filtros: Filtros, habilitado: boolean) {
  const [estado, setEstado] = useState<{ datos: PropiedadFeature[]; cargando: boolean; error: string | null }>({
    datos: [],
    cargando: true,
    error: null,
  })

  useEffect(() => {
    if (!habilitado) return
    const control = new AbortController()
    setEstado((e) => ({ ...e, cargando: true, error: null }))
    apiGet<GeoFeatureCollection<PropiedadFeature>>(
      '/api/propiedades',
      {
        departamentoId: filtros.departamentoId,
        ciudadId: filtros.ciudadId,
        barrioId: filtros.barrioId,
        categoria: filtros.categoria === 'Todos' ? null : filtros.categoria,
        operacion: filtros.tipoOperacion === 'Todos' ? null : filtros.tipoOperacion,
        presupuestoMax: filtros.presupuestoMax,
        moneda: filtros.presupuestoMax ? MONEDA_PRESUPUESTO : null,
      },
      control.signal
    )
      .then((fc) => setEstado({ datos: fc.features, cargando: false, error: null }))
      .catch((e: unknown) => {
        if (esCancelacion(e)) return
        setEstado((s) => ({
          ...s,
          cargando: false,
          error: e instanceof Error ? e.message : 'No se pudieron cargar las propiedades.',
        }))
      })
    return () => control.abort() // si cambian los filtros, se descarta la búsqueda anterior
  }, [
    habilitado,
    filtros.departamentoId,
    filtros.ciudadId,
    filtros.barrioId,
    filtros.categoria,
    filtros.tipoOperacion,
    filtros.presupuestoMax,
  ])

  return estado
}

// ─── Galería de fotos deslizable ─────────────────────────────────────────

export function ImageCarousel({ images }: { images: string[] }) {
  const trackRef = useRef<HTMLDivElement>(null)
  const [index, setIndex] = useState(0)

  function goTo(i: number, e?: React.MouseEvent) {
    e?.stopPropagation()
    const el = trackRef.current
    if (!el) return
    const clamped = (i + images.length) % images.length
    el.scrollTo({ left: clamped * el.clientWidth, behavior: 'smooth' })
    setIndex(clamped)
  }

  function handleScroll() {
    const el = trackRef.current
    if (!el || el.clientWidth === 0) return
    const i = Math.round(el.scrollLeft / el.clientWidth)
    setIndex(Math.min(Math.max(i, 0), images.length - 1))
  }

  return (
    <div className={styles.carousel}>
      <div ref={trackRef} className={styles.carouselTrack} onScroll={handleScroll}>
        {images.map((src, i) => (
          <img key={i} src={src} alt="" className={styles.carouselImg} draggable={false} />
        ))}
      </div>

      {images.length > 1 && (
        <>
          <button
            className={`${styles.carouselArrow} ${styles.carouselArrowLeft}`}
            onClick={(e) => goTo(index - 1, e)}
            aria-label="Foto anterior"
          >
            <ChevronLeft size={22} aria-hidden="true" />
          </button>
          <button
            className={`${styles.carouselArrow} ${styles.carouselArrowRight}`}
            onClick={(e) => goTo(index + 1, e)}
            aria-label="Foto siguiente"
          >
            <ChevronRight size={22} aria-hidden="true" />
          </button>
          <div className={styles.carouselDots}>
            {images.map((_, i) => (
              <button
                key={i}
                className={`${styles.carouselDot} ${i === index ? styles.carouselDotActive : ''}`}
                onClick={(e) => goTo(i, e)}
                aria-label={`Ir a foto ${i + 1}`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

export default function SearchFilter() {
  const { ciudades, barrios, propiedades, loading, error } = useGeoData()
  const [filtros, setFiltros] = useState<Filtros>(FILTROS_VACIOS)
  const [extraBounds, setExtraBounds] = useState<LatLngBounds | null>(null)
  const [propiedadId, setPropiedadId] = useState<string | null>(null)
  const [searchParams] = useSearchParams()

  // Permite deep-link desde el home/footer: /buscar?ciudad=01&barrio=10011013
  // y también /buscar?categoria=Terreno&tipoOperacion=Venta
  // y /buscar?propiedad=prop-005 (abre directo el detalle de esa propiedad,
  // usado por las tarjetas de "Ofertas" del inicio)
  useEffect(() => {
    if (loading) return
    const ciudadParam = searchParams.get('ciudad')
    const barrioParam = searchParams.get('barrio')
    const categoriaParam = searchParams.get('categoria')
    const tipoOperacionParam = searchParams.get('tipoOperacion')
    const propiedadParam = searchParams.get('propiedad')

    const categoriasValidas: Filtros['categoria'][] = ['Todos', 'Vivienda', 'Terreno']
    const operacionesValidas: Filtros['tipoOperacion'][] = ['Todos', 'Venta', 'Alquiler']
    const categoria = categoriasValidas.find((c) => c === categoriaParam)
    const tipoOperacion = operacionesValidas.find((t) => t === tipoOperacionParam)
    const propiedad = propiedadParam ? propiedades.find((p) => p.properties.id === propiedadParam) : undefined

    if (!ciudadParam && !barrioParam && !categoria && !tipoOperacion && !propiedad) return

    if (propiedad) {
      const ciudadDeLaProp = ciudades.find((c) => c.properties.ciudad === propiedad.properties.ciudad)
      setFiltros((f) => ({
        ...f,
        ciudadId: ciudadDeLaProp?.properties.id ?? f.ciudadId,
        barrioId: propiedad.properties.barrioId,
      }))
      setPropiedadId(propiedad.properties.id)
      return
    }

    setFiltros((f) => ({
      ...f,
      ciudadId: ciudadParam ?? f.ciudadId,
      barrioId: barrioParam ? Number(barrioParam) : f.barrioId,
      categoria: categoria ?? f.categoria,
      tipoOperacion: tipoOperacion ?? f.tipoOperacion,
    }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading])

  function patch(p: Partial<Filtros>) {
    setFiltros((f) => {
      const next = { ...f, ...p }
      // Un terreno sólo se compra: si eligen "Terreno" y había "Alquiler" cargado, se limpia.
      if (next.categoria === 'Terreno' && next.tipoOperacion === 'Alquiler') {
        next.tipoOperacion = 'Todos'
      }
      return next
    })
    setExtraBounds(null)
    setPropiedadId(null)
  }

  // Departamentos disponibles, a partir de las ciudades que entrega la API
  const departamentos = useMemo(() => {
    const vistos = new Map<number, string>()
    ciudades.forEach((c) => {
      if (c.properties.departamentoId != null) vistos.set(c.properties.departamentoId, c.properties.departamento)
    })
    return [...vistos.entries()].map(([id, nombre]) => ({ id, nombre }))
  }, [ciudades])

  const ciudadesVisibles = useMemo(
    () =>
      filtros.departamentoId == null
        ? ciudades
        : ciudades.filter((c) => c.properties.departamentoId === filtros.departamentoId),
    [ciudades, filtros.departamentoId]
  )

  const barriosDeLaCiudad = useMemo(
    () => barrios.filter((b) => b.properties.distritoCod === filtros.ciudadId),
    [barrios, filtros.ciudadId]
  )

  const ciudadSeleccionada = ciudades.find((c) => c.properties.id === filtros.ciudadId)
  const barrioSeleccionado = barriosDeLaCiudad.find((b) => b.properties.id === filtros.barrioId)
  const nivel = filtros.barrioId ? 'barrio' : filtros.ciudadId ? 'ciudad' : 'departamento'

  // Los filtros los aplica el backend (/api/propiedades?...): ciudad, barrio, tipo, operación,
  // presupuesto en USD y departamento. Aquí solo se muestran los resultados.
  const busqueda = usePropiedadesFiltradas(filtros, !loading && !error)
  const propiedadesFiltradas = busqueda.datos
  const propiedadesDelBarrio = propiedadesFiltradas
  const resultados = propiedadesFiltradas

  const propiedadSeleccionada = useMemo(
    () => propiedades.find((p) => p.properties.id === propiedadId) ?? null,
    [propiedades, propiedadId]
  )

  const barrioDeLaPropiedad = propiedadSeleccionada
    ? barrios.find((b) => b.properties.id === propiedadSeleccionada.properties.barrioId)
    : undefined

  const flyToBounds: LatLngBounds | null = useMemo(() => {
    if (propiedadSeleccionada) {
      const [lng, lat] = propiedadSeleccionada.geometry.coordinates
      return boundsForPoint(lat, lng)
    }
    if (filtros.barrioId) return boundsForBarrio(barrios, filtros.barrioId)
    if (filtros.ciudadId) return boundsForCiudad(barrios, filtros.ciudadId)
    if (extraBounds) return extraBounds
    // Vista general: el mapa se encuadra en todas las propiedades (antes usaba un centro fijo, desplazado al oeste)
    return propiedades.length > 0 ? boundsFromFeatures(propiedades) : null
  }, [barrios, propiedades, filtros.ciudadId, filtros.barrioId, extraBounds, propiedadSeleccionada])

  function handleBuscar() {
    setPropiedadId(null)
    if (!filtros.ciudadId && propiedadesFiltradas.length > 0) {
      setExtraBounds(boundsFromFeatures(propiedadesFiltradas))
    }
  }

  function seleccionarPropiedad(p: PropiedadFeature) {
    const props = p.properties
    // Si tocan un pin fuera del contexto actual, ubicamos ciudad/barrio para
    // que el botón "volver" funcione de forma coherente.
    if (props.barrioId !== filtros.barrioId) {
      const ciudadDeLaProp = ciudades.find((c) => c.properties.ciudad === props.ciudad)
      setFiltros((f) => ({
        ...f,
        ciudadId: ciudadDeLaProp?.properties.id ?? f.ciudadId,
        barrioId: props.barrioId,
      }))
    }
    setPropiedadId(props.id)
  }

  function volverDesdeDetalle() {
    setPropiedadId(null)
  }

  function volverDesdeBarrio() {
    patch({ barrioId: null })
  }

  function volverDesdeCiudad() {
    patch({ ciudadId: null, barrioId: null })
  }

  if (loading) {
    return (
      <section className={styles.page}>
        <div className={styles.centeredMsg}>Cargando mapa…</div>
      </section>
    )
  }

  if (error) {
    return (
      <section className={styles.page}>
        <div className={`${styles.centeredMsg} ${styles.centeredMsgError}`}>{error}</div>
      </section>
    )
  }

  return (
    <section className={styles.page}>
      {/* ── Barra de filtros (sin barrio hasta elegir ciudad) ───────────── */}
      <div className={styles.filterBar}>
        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>Departamento</label>
          <select
            className={styles.filterSelect}
            value={filtros.departamentoId ?? ''}
            onChange={(e) =>
              patch({ departamentoId: e.target.value ? Number(e.target.value) : null, ciudadId: null, barrioId: null })
            }
          >
            <option value="">Todos</option>
            {departamentos.map((d) => (
              <option key={d.id} value={d.id}>
                {d.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>Ciudad</label>
          <select
            className={styles.filterSelect}
            value={filtros.ciudadId ?? ''}
            onChange={(e) => patch({ ciudadId: e.target.value || null, barrioId: null })}
          >
            <option value="">Todas</option>
            {ciudadesVisibles.map((c) => (
              <option key={c.properties.id} value={c.properties.id}>
                {c.properties.ciudad}
              </option>
            ))}
          </select>
        </div>

        {filtros.ciudadId && (
          <div className={styles.filterGroup}>
            <label className={styles.filterLabel}>Barrio</label>
            <select
              className={styles.filterSelect}
              value={filtros.barrioId ?? ''}
              onChange={(e) => patch({ barrioId: e.target.value ? Number(e.target.value) : null })}
            >
              <option value="">Todos</option>
              {barriosDeLaCiudad.map((b) => (
                <option key={b.properties.id} value={b.properties.id}>
                  {b.properties.barrio}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>Tipo de propiedad</label>
          <select
            className={styles.filterSelect}
            value={filtros.categoria}
            onChange={(e) => patch({ categoria: e.target.value as Filtros['categoria'] })}
          >
            <option value="Todos">Todos</option>
            <option value="Vivienda">Vivienda (casa / depto)</option>
            <option value="Terreno">Terreno</option>
          </select>
        </div>

        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>Operación</label>
          <select
            className={styles.filterSelect}
            value={filtros.tipoOperacion}
            onChange={(e) => patch({ tipoOperacion: e.target.value as Filtros['tipoOperacion'] })}
          >
            <option value="Todos">Todos</option>
            <option value="Venta">Venta</option>
            {filtros.categoria !== 'Terreno' && <option value="Alquiler">Alquiler</option>}
          </select>
        </div>

        <div className={styles.filterGroup}>
          <label className={styles.filterLabel}>Presupuesto</label>
          <select
            className={styles.filterSelect}
            value={filtros.presupuestoMax ?? ''}
            onChange={(e) => patch({ presupuestoMax: e.target.value ? Number(e.target.value) : null })}
          >
            <option value="">Todos</option>
            {PRESUPUESTOS.map((p) => (
              <option key={p.value} value={p.value}>
                {p.label}
              </option>
            ))}
          </select>
        </div>

        <button className={styles.searchBtn} onClick={handleBuscar}>
          Buscar
        </button>
      </div>

      {busqueda.error && (
        <p className={styles.emptyText} role="alert">
          {busqueda.error}
        </p>
      )}

      {/* ── Escenario principal: mapa a pantalla completa ───────────────── */}
      <div className={styles.stage}>
        {propiedadSeleccionada ? (
          <DetalleView
            propiedad={propiedadSeleccionada}
            barrio={barrioDeLaPropiedad}
            flyToBounds={flyToBounds}
            onVolver={volverDesdeDetalle}
          />
        ) : nivel === 'barrio' && barrioSeleccionado ? (
          <BarrioView
            ciudades={ciudades}
            barriosVisibles={barriosDeLaCiudad}
            barrioSeleccionado={barrioSeleccionado}
            ciudadSeleccionada={ciudadSeleccionada}
            propiedades={propiedadesDelBarrio}
            flyToBounds={flyToBounds}
            barrioId={filtros.barrioId}
            onVolver={volverDesdeBarrio}
            onSelectBarrio={(id) => patch({ barrioId: id })}
            onSelectPropiedad={seleccionarPropiedad}
          />
        ) : (
          <MapaGeneralView
            ciudades={ciudadesVisibles}
            barriosVisibles={barriosDeLaCiudad}
            propiedades={resultados}
            nivel={filtros.ciudadId ? 'ciudad' : 'departamento'}
            ciudadSeleccionada={ciudadSeleccionada}
            flyToBounds={flyToBounds}
            onVolver={volverDesdeCiudad}
            onSelectCiudad={(id) => patch({ ciudadId: id, barrioId: null })}
            onSelectBarrio={(id) => patch({ barrioId: id })}
            onSelectPropiedad={seleccionarPropiedad}
          />
        )}
      </div>
    </section>
  )
}

// ─── Nivel "departamento" / "ciudad": mapa a pantalla completa ──────────────

function MapaGeneralView({
  ciudades,
  barriosVisibles,
  propiedades,
  nivel,
  ciudadSeleccionada,
  flyToBounds,
  onVolver,
  onSelectCiudad,
  onSelectBarrio,
  onSelectPropiedad,
}: {
  ciudades: ReturnType<typeof useGeoData>['ciudades']
  barriosVisibles: ReturnType<typeof useGeoData>['barrios']
  propiedades: PropiedadFeature[]
  nivel: 'departamento' | 'ciudad'
  ciudadSeleccionada: ReturnType<typeof useGeoData>['ciudades'][number] | undefined
  flyToBounds: LatLngBounds | null
  onVolver: () => void
  onSelectCiudad: (id: string) => void
  onSelectBarrio: (id: number) => void
  onSelectPropiedad: (p: PropiedadFeature) => void
}) {
  return (
    <>
      {nivel === 'ciudad' && (
        <div className={styles.stageHeader}>
          <h2 className={styles.stageTitle}>
            Tenemos propiedades en los barrios de{' '}
            <em className={styles.stageTitleAccent}>{ciudadSeleccionada?.properties.ciudad}</em>:
          </h2>
        </div>
      )}
      <div className={styles.mapOnly}>
        {nivel === 'ciudad' && (
          <div className="pm-breadcrumb">
            <button className="pm-back-btn" onClick={onVolver}>
              <ChevronLeft size={16} aria-hidden="true" /> Volver
            </button>
            <span>Alto Paraná › {ciudadSeleccionada?.properties.ciudad}</span>
          </div>
        )}
        <MapCanvas
          ciudades={ciudades}
          barriosVisibles={barriosVisibles}
          propiedadesVisibles={propiedades}
          showCiudades={nivel === 'departamento'}
          showBarrios={nivel === 'ciudad'}
          showPropiedades
          barriosLayerKey={`sf-${ciudadSeleccionada?.properties.id ?? 'none'}`}
          selectedBarrioId={null}
          flyToBounds={flyToBounds}
          onSelectCiudad={onSelectCiudad}
          onSelectBarrio={(id) => onSelectBarrio(id)}
          onSelectPropiedad={(id) => {
            const p = propiedades.find((x) => x.properties.id === id)
            if (p) onSelectPropiedad(p)
          }}
        />
        <p className={styles.mapHint}>
          {nivel === 'departamento'
            ? 'Hacé clic en una ciudad, o filtrá por zona, para ver sus barrios'
            : 'Hacé clic en un barrio pintado para ver sus propiedades'}
        </p>
      </div>
    </>
  )
}

// ─── Nivel "barrio": mapa con barrios pintados + panel deslizable ───────────

function BarrioView({
  ciudades,
  barriosVisibles,
  barrioSeleccionado,
  ciudadSeleccionada,
  propiedades,
  flyToBounds,
  barrioId,
  onVolver,
  onSelectBarrio,
  onSelectPropiedad,
}: {
  ciudades: ReturnType<typeof useGeoData>['ciudades']
  barriosVisibles: ReturnType<typeof useGeoData>['barrios']
  barrioSeleccionado: ReturnType<typeof useGeoData>['barrios'][number]
  ciudadSeleccionada: ReturnType<typeof useGeoData>['ciudades'][number] | undefined
  propiedades: PropiedadFeature[]
  flyToBounds: LatLngBounds | null
  barrioId: number | null
  onVolver: () => void
  onSelectBarrio: (id: number) => void
  onSelectPropiedad: (p: PropiedadFeature) => void
}) {
  // Una casa con Venta y Alquiler cuenta en ambos
  const enVenta = propiedades.filter((p) => tieneOperacion(p.properties, 'Venta')).length
  const enAlquiler = propiedades.filter((p) => tieneOperacion(p.properties, 'Alquiler')).length

  return (
    <>
      <div className={styles.stageHeader}>
        <h2 className={styles.stageTitle}>
          Tenemos propiedades en los barrios/zona: <em className={styles.stageTitleAccent}>{barrioSeleccionado.properties.barrio}</em>
        </h2>
      </div>

      <div className={styles.splitStage}>
        {/* Mapa con los barrios pintados */}
        <div className={styles.mapPane}>
          <div className="pm-breadcrumb">
            <button className="pm-back-btn" onClick={onVolver}>
              <ChevronLeft size={16} aria-hidden="true" /> Volver
            </button>
            <span>
              Alto Paraná › {ciudadSeleccionada?.properties.ciudad} › {barrioSeleccionado.properties.barrio}
            </span>
          </div>
          <MapCanvas
            ciudades={ciudades}
            barriosVisibles={barriosVisibles}
            propiedadesVisibles={propiedades}
            showCiudades={false}
            showBarrios
            showPropiedades
            barriosLayerKey={`sf-barrio-${ciudadSeleccionada?.properties.id ?? 'none'}`}
            selectedBarrioId={barrioId}
            flyToBounds={flyToBounds}
            onSelectCiudad={() => {}}
            onSelectBarrio={onSelectBarrio}
            onSelectPropiedad={(id) => {
              const p = propiedades.find((x) => x.properties.id === id)
              if (p) onSelectPropiedad(p)
            }}
          />
        </div>

        {/* Panel deslizable: nombre/descripción del barrio + grilla de propiedades */}
        <div className={styles.sidePanel}>
          <div className={styles.sidePanelHeader}>
            <h3 className={styles.sidePanelTitle}>{barrioSeleccionado.properties.barrio}</h3>
            <p className={styles.sidePanelDesc}>
              Barrio {barrioSeleccionado.properties.barrio.toLowerCase()}, en{' '}
              {ciudadSeleccionada?.properties.ciudad}, departamento de Alto Paraná.
            </p>
            <div className={styles.sidePanelStats}>
              <span className={styles.sidePanelStat}>{propiedades.length} propiedad(es)</span>
              <span className={styles.sidePanelStat}>{enVenta} en venta</span>
              <span className={styles.sidePanelStat}>{enAlquiler} en alquiler</span>
            </div>
          </div>

          <div className={styles.sidePanelScroll}>
            <span className={styles.sidePanelScrollLabel}>Propiedades que hay en este barrio</span>
            {propiedades.length === 0 ? (
              <p className={styles.emptyText}>
                No hay propiedades cargadas para esta selección todavía. Probá quitando algún
                filtro.
              </p>
            ) : (
              <div className={styles.cardGrid}>
                {propiedades.map((p) => (
                  <PropiedadCard key={p.properties.id} p={p} onClick={() => onSelectPropiedad(p)} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  )
}

function PropiedadCard({ p, onClick }: { p: PropiedadFeature; onClick: () => void }) {
  const props = p.properties
  const fotos = fotosPara(props)

  return (
    <div className={styles.propCard}>
      <div className={styles.propCardImg}>
        {fotos.length > 0 ? (
          <img src={fotos[0]} alt={props.titulo} className={styles.propThumbImg} />
        ) : (
          <span className={styles.propCardIcon}>{iconoCategoria(props.categoria)}</span>
        )}
        <span className={styles.propBadge}>{props.categoria}</span>
      </div>
      <div
        className={styles.propBody}
        role="button"
        tabIndex={0}
        onClick={onClick}
        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && onClick()}
      >
        <div className={styles.propTitle}>{props.titulo}</div>
        {operacionesDe(props).map((op) => (
          <div key={op.operacion} className={styles.propPrice}>
            {operacionesDe(props).length > 1 ? `${op.operacion}: ` : ''}
            {textoPrecio(op)}
          </div>
        ))}
        <div className={styles.propSpecs}>
          {props.categoria !== 'Terreno' && (
            <>
              <span className={styles.propSpec}><BedDouble size={15} aria-hidden="true" /> {props.dormitorios}</span>
              <span className={styles.propSpec}><Bath size={15} aria-hidden="true" /> {props.banos}</span>
            </>
          )}
          <span className={styles.propSpec}><Ruler size={15} aria-hidden="true" /> {props.m2} m²</span>
        </div>
      </div>
    </div>
  )
}

// ─── Detalle de propiedad: ubicación exacta + fotos/especificaciones ───────

function DetalleView({
  propiedad,
  barrio,
  flyToBounds,
  onVolver,
}: {
  propiedad: PropiedadFeature
  barrio: ReturnType<typeof useGeoData>['barrios'][number] | undefined
  flyToBounds: LatLngBounds | null
  onVolver: () => void
}) {
  const props = propiedad.properties
  const esTerreno = props.categoria === 'Terreno'
  const fotos = fotosPara(props)
  const operaciones = operacionesDe(props)

  return (
    <div className={styles.splitStage}>
      {/* Ubicación exacta de la propiedad */}
      <div className={styles.mapPane}>
        <div className="pm-breadcrumb">
          <button className="pm-back-btn" onClick={onVolver}>
            <ChevronLeft size={16} aria-hidden="true" /> Volver al barrio
          </button>
          <span>{props.direccion}</span>
        </div>
        <MapCanvas
          ciudades={[]}
          barriosVisibles={barrio ? [barrio] : []}
          propiedadesVisibles={[propiedad]}
          showCiudades={false}
          showBarrios={false}
          showPropiedades
          barriosLayerKey={`sf-detalle-${props.id}`}
          selectedBarrioId={null}
          flyToBounds={flyToBounds}
          onSelectCiudad={() => {}}
          onSelectBarrio={() => {}}
        />
      </div>

      {/* Fotos + especificaciones (deslizable) */}
      <div className={styles.sidePanel}>
        <div className={styles.detailPhotos}>
          {fotos.length > 0 ? (
            <ImageCarousel images={fotos} />
          ) : (
            <span className={styles.detailPhotosIcon}>{iconoCategoria(props.categoria)}</span>
          )}
          <button className={styles.detailCloseBtn} onClick={onVolver} aria-label="Volver al barrio">
            <X size={20} aria-hidden="true" />
          </button>
          <span className={styles.detailPhotosTag}>
            {operaciones.map((o) => o.operacion.toUpperCase()).join(' · ')}
          </span>
        </div>

        <div className={styles.sidePanelScroll}>
          <span className={styles.sidePanelScrollLabel}>Especificaciones de la propiedad</span>

          <h3 className={styles.detailTitle}>{props.titulo}</h3>
          {operaciones.map((op) => (
            <div key={op.operacion} className={styles.detailPrice}>
              {operaciones.length > 1 ? `${op.operacion}: ` : ''}
              {textoPrecio(op)}
            </div>
          ))}

          <div className={styles.specGrid}>
            {!esTerreno && (
              <>
                <SpecTile label="Dormitorios" value={props.dormitorios} />
                <SpecTile label="Baños" value={props.banos} />
              </>
            )}
            <SpecTile label="Superficie" value={`${props.m2} m²`} />
            <SpecTile label="Operación" value={operaciones.map((o) => o.operacion).join(' y ')} />
            <SpecTile label="Barrio" value={props.barrio} />
            <SpecTile label="Ciudad" value={props.ciudad} />
          </div>

          {props.descripcion && <p className={styles.detailAddress}>{props.descripcion}</p>}

          <p className={styles.detailAddress}><MapPin size={16} aria-hidden="true" /> {props.direccion}</p>

          <button
            className={styles.searchBtn}
            onClick={() => abrirAsistenteConPropiedad({ codigo: props.id, titulo: props.titulo })}
          >
            <MessageCircle size={18} aria-hidden="true" /> Consultar sobre esta propiedad
          </button>
        </div>
      </div>
    </div>
  )
}

function SpecTile({ label, value }: { label: string; value: string | number }) {
  return (
    <div className={styles.specTile}>
      <div className={styles.specValue}>{value}</div>
      <div className={styles.specLabel}>{label}</div>
    </div>
  )
}

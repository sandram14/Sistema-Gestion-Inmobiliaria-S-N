import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SearchFilter from '../src/components/busqueda/SearchFilter'
import { soloConPropiedades } from '../src/components/PropertyMap/useGeoData'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import { fetchFalso, pedidos, control } from './backendFalso'

// El mapa se reemplaza por botones con las ciudades y los barrios que recibe
vi.mock('../src/components/PropertyMap/MapCanvas', () => ({
  default: (p: any) => (
    <div>
      {p.ciudades.map((c: any) => <button key={c.properties.id} onClick={() => p.onSelectCiudad(c.properties.id)}>mapa ciudad {c.properties.ciudad} ({c.properties.totalBarrios})</button>)}
      {p.barriosVisibles.map((b: any) => <span key={b.properties.id}>mapa barrio {b.properties.barrio}</span>)}
    </div>
  ),
}))
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any

const cuadrado = (x: number, y: number) => ({ type: 'Polygon', coordinates: [[[x, y], [x + 0.01, y], [x + 0.01, y + 0.01], [x, y]]] })
const ciudad = (id: string, nombre: string, total: number) =>
  ({ type: 'Feature', geometry: { type: 'Point', coordinates: [-54.6, -25.5] }, properties: { id, ciudad: nombre, departamento: 'ALTO PARANÁ', totalBarrios: total, color: '#000', departamentoId: 1 } }) as any
const barrio = (id: number, nombre: string, distritoCod: string) =>
  ({ type: 'Feature', geometry: cuadrado(-54.6, -25.5), properties: { id, barrio: nombre, distritoCod, ciudad: '', departamento: 'ALTO PARANÁ', color: '#000' } }) as any
const propiedad = (barrioId: number) => ({ type: 'Feature', geometry: { type: 'Point', coordinates: [0, 0] }, properties: { barrioId } }) as any

// El backend entrega las ciudades con propiedades y TODOS sus barrios: se agregan un barrio vacío en Ciudad del Este
// y una ciudad (Minga Guazú) sin ninguna propiedad, como pasaría con una copia guardada en el navegador.
async function fetchConZonasVacias(url: string, init?: RequestInit) {
  const r = await fetchFalso(url, init)
  const ruta = new URL(url, 'http://x').pathname
  if (ruta !== '/api/barrios' && ruta !== '/api/ciudades') return r
  const datos = await r.json()
  if (ruta === '/api/barrios') datos.features.push(barrio(10011099, 'BARRIO SIN PROPIEDADES', '1'), barrio(10071001, 'KM 16', '7'))
  else datos.features.push(ciudad('7', 'MINGA GUAZÚ', 1))
  return new Response(JSON.stringify(datos), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

const selectDe = (etiqueta: string) => screen.getByText(etiqueta).parentElement!.querySelector('select') as HTMLSelectElement
const opciones = (s: HTMLSelectElement) => [...s.options].map((o) => o.textContent)

beforeEach(() => {
  reiniciarContenidoSitio(); cleanup(); pedidos.length = 0; control.caido = false; control.erroresBusqueda = null
  vi.stubGlobal('fetch', vi.fn(fetchConZonasVacias))
})

describe('Zonas del sitio: solo donde hay propiedades disponibles', () => {
  it('soloConPropiedades deja los barrios con propiedades y sus ciudades, y recalcula la cantidad de barrios', () => {
    const r = soloConPropiedades(
      [ciudad('1', 'CIUDAD DEL ESTE', 40), ciudad('3', 'HERNANDARIAS', 20), ciudad('7', 'MINGA GUAZÚ', 10)],
      [barrio(1, 'A', '1'), barrio(2, 'B', '1'), barrio(3, 'C', '1'), barrio(4, 'D', '3'), barrio(5, 'E', '7')],
      [propiedad(1), propiedad(1), propiedad(3), propiedad(4)],
    )
    expect(r.barrios.map((b) => b.properties.barrio)).toEqual(['A', 'C', 'D'])
    expect(r.ciudades.map((c) => [c.properties.ciudad, c.properties.totalBarrios])).toEqual([['CIUDAD DEL ESTE', 2], ['HERNANDARIAS', 1]])
    expect(soloConPropiedades([ciudad('1', 'X', 3)], [barrio(1, 'A', '1')], [])).toEqual({ ciudades: [], barrios: [] })
  })

  it('las listas Ciudad y Barrio y el mapa no muestran zonas sin propiedades', async () => {
    render(<MemoryRouter initialEntries={['/buscar']}><SearchFilter /></MemoryRouter>)
    await screen.findByText('mapa ciudad CIUDAD DEL ESTE (1)')                     // la cantidad cuenta solo barrios con propiedades
    expect(screen.queryByText(/MINGA GUAZÚ/)).toBeNull()                           // ni en el mapa ni en la lista
    expect(opciones(selectDe('Ciudad'))).toEqual(['Todas', 'CIUDAD DEL ESTE', 'HERNANDARIAS'])

    fireEvent.change(selectDe('Ciudad'), { target: { value: '1' } })
    expect(opciones(selectDe('Barrio'))).toEqual(['Todos', 'AREA 1'])
    fireEvent.click(screen.getByText('mapa ciudad CIUDAD DEL ESTE (1)'))
    expect(await screen.findByText('mapa barrio AREA 1')).toBeTruthy()
    expect(screen.queryByText('mapa barrio BARRIO SIN PROPIEDADES')).toBeNull()
  })
})

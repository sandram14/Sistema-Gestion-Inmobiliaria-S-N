import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SearchFilter from '../src/components/busqueda/SearchFilter'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import { fetchFalso, pedidos, control } from './backendFalso'

// El mapa se reemplaza por botones que hacen lo mismo que los clics en ciudades, barrios y marcadores.
// "mover mapa" simula que el cliente acerca el mapa; la vista restaurada se muestra como texto.
vi.mock('../src/components/PropertyMap/MapCanvas', () => ({
  default: (p: any) => (
    <div>
      {p.showCiudades && p.ciudades.map((c: any) => <button key={c.properties.id} onClick={() => p.onSelectCiudad(c.properties.id)}>mapa ciudad {c.properties.ciudad}</button>)}
      {p.showBarrios && p.barriosVisibles.map((b: any) => <button key={b.properties.id} onClick={() => p.onSelectBarrio(b.properties.id, b.properties.barrio)}>mapa barrio {b.properties.barrio}</button>)}
      {p.onSelectPropiedad && p.propiedadesVisibles.map((x: any) => <button key={x.properties.id} onClick={() => p.onSelectPropiedad(x.properties.id)}>marcador {x.properties.titulo}</button>)}
      {p.onVistaCambia && <button onClick={() => p.onVistaCambia({ centro: [-25.51, -54.61], zoom: 17 })}>mover mapa</button>}
      {p.vistaRestaurada && <span>vista restaurada {p.vistaRestaurada.zoom}</span>}
      {p.selectedBarrioId != null && <span>barrio resaltado {p.selectedBarrioId}</span>}
    </div>
  ),
}))
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any

const app = (ruta = '/buscar') => render(<MemoryRouter initialEntries={[ruta]}><SearchFilter /></MemoryRouter>)
const ultimaBusqueda = () => [...pedidos].reverse().find((p) => p.url.startsWith('/api/propiedades'))!.url
const selectDe = (etiqueta: string) => screen.getByText(etiqueta).parentElement!.querySelector('select') as HTMLSelectElement
// La X del detalle (el botón con texto del mapa tiene el mismo nombre)
const cerrarDetalle = () => fireEvent.click(document.querySelector('button[aria-label="Volver a los resultados"]')!)
const ruta = () => document.querySelector('.pm-breadcrumb span')?.textContent

beforeEach(() => {
  reiniciarContenidoSitio(); cleanup(); pedidos.length = 0; control.caido = false; control.erroresBusqueda = null
  vi.stubGlobal('fetch', vi.fn(fetchFalso))
})

describe('Barrio sin panel lateral: el mapa se acerca y muestra sus marcadores', () => {
  it('con un clic en el mapa: ciudad → barrio (sin ficha ni títulos) → marcador → detalle', async () => {
    app(); await screen.findByText('Departamento')
    fireEvent.click(await screen.findByText('mapa ciudad CIUDAD DEL ESTE'))
    expect(screen.queryByText(/Tenemos propiedades en los barrios/)).toBeNull()      // sin texto sobre el mapa
    fireEvent.click(await screen.findByText('mapa barrio AREA 1'))
    await waitFor(() => expect(ultimaBusqueda()).toContain('barrioId=10011013'))
    expect(ruta()).toBe('Alto Paraná › CIUDAD DEL ESTE › AREA 1')                   // la ruta y el botón Volver siguen
    expect(screen.getByRole('button', { name: /^Volver$/ })).toBeTruthy()
    expect(screen.getByText('barrio resaltado 10011013')).toBeTruthy()
    expect(screen.queryByText('1 en venta')).toBeNull()                              // ya no hay panel del barrio
    expect(screen.queryByText(/Propiedades que hay en este barrio/)).toBeNull()
    expect(screen.queryByText(/Tenemos propiedades en los barrios/)).toBeNull()
    expect(screen.getByText('Hacé clic en un marcador para ver la propiedad')).toBeTruthy()
    fireEvent.click(await screen.findByText('marcador Casa en Area 1'))
    expect(await screen.findByText('Casa luminosa con patio.')).toBeTruthy()
  })
  it('con el buscador: ciudad y barrio + Buscar llevan al mismo mapa del barrio', async () => {
    app(); await screen.findByText('Departamento')
    fireEvent.change(selectDe('Ciudad'), { target: { value: '1' } })
    fireEvent.change(selectDe('Barrio'), { target: { value: '10011013' } })
    fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => expect(ultimaBusqueda()).toContain('barrioId=10011013'))
    expect(ruta()).toBe('Alto Paraná › CIUDAD DEL ESTE › AREA 1')
    expect(screen.queryByText('1 en venta')).toBeNull()
    expect(await screen.findByText('marcador Casa en Area 1')).toBeTruthy()
  })
  it('"Volver" en el barrio vuelve a la ciudad y en la ciudad al mapa general', async () => {
    app('/buscar?ciudad=1&barrio=10011013'); await waitFor(() => expect(ruta()).toBe('Alto Paraná › CIUDAD DEL ESTE › AREA 1'))
    fireEvent.click(screen.getByRole('button', { name: /^Volver$/ }))
    await waitFor(() => expect(ruta()).toBe('Alto Paraná › CIUDAD DEL ESTE'))
    expect(ultimaBusqueda()).not.toContain('barrioId')
    fireEvent.click(screen.getByRole('button', { name: /^Volver$/ }))
    await waitFor(() => expect(screen.getByText('mapa ciudad CIUDAD DEL ESTE')).toBeTruthy())
  })
  it('si los filtros no dejan propiedades en el barrio, lo dice debajo del mapa', async () => {
    app('/buscar?ciudad=1&barrio=10011013&categoria=Terreno')
    expect(await screen.findByText(/No hay propiedades en este barrio con estos filtros/)).toBeTruthy()
  })
})

describe('La X del detalle vuelve al mapa como estaba (barrio, zoom, filtros y barra)', () => {
  it('desde el barrio (clic en el mapa): vuelve al barrio con el mismo zoom y los filtros de Buscar', async () => {
    app(); await screen.findByText('Departamento')
    fireEvent.change(selectDe('Tipo de propiedad'), { target: { value: 'Vivienda' } }); fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => expect(ultimaBusqueda()).toContain('categoria=Vivienda'))
    fireEvent.click(await screen.findByText('mapa ciudad CIUDAD DEL ESTE'))
    fireEvent.click(await screen.findByText('mapa barrio AREA 1'))
    fireEvent.click(screen.getByText('mover mapa'))                                   // el cliente acerca el mapa (zoom 17)
    fireEvent.click(await screen.findByText('marcador Casa en Area 1'))
    await screen.findByText('Casa luminosa con patio.')
    cerrarDetalle()
    await waitFor(() => expect(ruta()).toBe('Alto Paraná › CIUDAD DEL ESTE › AREA 1')) // el mismo barrio
    expect(screen.getByText('vista restaurada 17')).toBeTruthy()                       // el mismo zoom
    const u = ultimaBusqueda(); expect(u).toContain('barrioId=10011013'); expect(u).toContain('categoria=Vivienda')
    expect(selectDe('Barrio').value).toBe('10011013'); expect(selectDe('Tipo de propiedad').value).toBe('Vivienda')
    expect(screen.queryByText('1 en venta')).toBeNull()
  })
  it('desde el barrio elegido con el buscador y presupuesto: vuelve igual, con el presupuesto en la barra', async () => {
    app(); await screen.findByText('Departamento')
    fireEvent.change(selectDe('Ciudad'), { target: { value: '1' } })
    fireEvent.change(selectDe('Barrio'), { target: { value: '10011013' } })
    fireEvent.change(screen.getByLabelText('Hasta (Gs.)'), { target: { value: '500000000' } })
    fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => expect(ultimaBusqueda()).toContain('presupuestoMax=500000000'))
    fireEvent.click(await screen.findByText('marcador Casa en Area 1'))
    await screen.findByText('Casa luminosa con patio.')
    cerrarDetalle()
    await waitFor(() => expect(ruta()).toBe('Alto Paraná › CIUDAD DEL ESTE › AREA 1'))
    expect((screen.getByLabelText('Hasta (Gs.)') as HTMLInputElement).value).toBe('500.000.000')
    expect(ultimaBusqueda()).toContain('barrioId=10011013')
  })
  it('desde el mapa general (marcador sin elegir ciudad): vuelve al mapa general, no al barrio de la propiedad', async () => {
    app(); await screen.findByText('Departamento')
    fireEvent.click(await screen.findByText('marcador Casa en Area 1'))
    await screen.findByText('Casa luminosa con patio.')
    cerrarDetalle()
    await waitFor(() => expect(screen.getByText('mapa ciudad CIUDAD DEL ESTE')).toBeTruthy())
    expect(ruta()).toBeUndefined()
    expect(ultimaBusqueda()).not.toMatch(/ciudadId|barrioId/)
  })
  it('desde una oferta de la portada (enlace directo a la propiedad), la X lleva al mapa general', async () => {
    app('/buscar?propiedad=prop-001'); await screen.findByText('Casa luminosa con patio.')
    cerrarDetalle()
    await waitFor(() => expect(screen.getByText('mapa ciudad CIUDAD DEL ESTE')).toBeTruthy())
    expect(ultimaBusqueda()).not.toMatch(/ciudadId|barrioId/)
  })
  it('después de volver, cualquier otro movimiento (otro barrio) vuelve a encuadrar el mapa normalmente', async () => {
    app('/buscar?ciudad=1&barrio=10011013'); fireEvent.click(await screen.findByText('mover mapa'))
    fireEvent.click(await screen.findByText('marcador Casa en Area 1')); await screen.findByText('Casa luminosa con patio.')
    cerrarDetalle(); expect(await screen.findByText('vista restaurada 17')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /^Volver$/ }))
    await waitFor(() => expect(screen.queryByText(/vista restaurada/)).toBeNull())
  })
})

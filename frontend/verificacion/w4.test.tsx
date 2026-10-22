import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import type { ReactNode } from 'react'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import SearchFilter from '../src/components/busqueda/SearchFilter'
import AssistantWidget from '../src/components/chat/AssistantWidget'
import OffersSection from '../src/components/home/OffersSection'
import { conPuntosDeMiles, montoDeCampo, textoDeCampo, textoGuaranies, textoPrecioOperacion } from '../src/services/guaranies'
import { fetchFalso, pedidos, control } from './backendFalso'

// El mapa real se reemplaza en el buscador; para probar el texto del tooltip se renderiza
// MapCanvas con react-leaflet simulado (marcador y tooltip como elementos simples).
vi.mock('../src/components/PropertyMap/MapCanvas', async (original) => {
  const real = await original<typeof import('../src/components/PropertyMap/MapCanvas')>()
  return { default: () => null, MapCanvasReal: real.default }
})
vi.mock('react-leaflet', () => {
  const Hijos = ({ children }: { children?: ReactNode }) => <div>{children}</div>
  return { MapContainer: Hijos, Marker: Hijos, Tooltip: ({ children }: { children?: ReactNode }) => <span data-testid="tooltip">{children}</span>,
    TileLayer: () => null, GeoJSON: () => null, ZoomControl: () => null, useMap: () => ({ flyToBounds() {} }) }
})
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any

const CONTACTO_REAL = { nombreEmpresa: 'S&N', descripcion: '', direccion: '', ciudad: '', email: 'info@sn.com.py', telefono: '0981', whatsapp: '595981111222', horario: '' }
const app = (ruta = '/buscar') => render(<MemoryRouter initialEntries={[ruta]}><SearchFilter /><AssistantWidget /></MemoryRouter>)
const chat = () => render(<MemoryRouter><AssistantWidget /></MemoryRouter>)
const busquedas = () => pedidos.filter((p) => p.url.startsWith('/api/propiedades')).map((p) => p.url)
const ultimaBusqueda = () => busquedas().at(-1)!
const desde = () => screen.getByLabelText('Desde (Gs.)') as HTMLInputElement
const hasta = () => screen.getByLabelText('Hasta (Gs.)') as HTMLInputElement
const escribir = (campo: HTMLInputElement, valor: string) => fireEvent.change(campo, { target: { value: valor } })

beforeEach(() => {
  reiniciarContenidoSitio(); cleanup(); pedidos.length = 0
  control.caido = false; control.respuestaAsistente = null; control.sitio = null; control.respuestaCliente = null; control.erroresBusqueda = null
  vi.stubGlobal('fetch', vi.fn(fetchFalso))
})

describe('Formato en guaraníes (services/guaranies.ts)', () => {
  it('punto de miles, sin decimales y "/ mes" en alquiler', () => {
    expect(conPuntosDeMiles(450000000)).toBe('450.000.000')
    expect(textoGuaranies(4000000.4)).toBe('Gs. 4.000.000')
    expect(textoPrecioOperacion('Venta', 280000000)).toBe('Gs. 280.000.000')
    expect(textoPrecioOperacion('Alquiler', 4000000)).toBe('Gs. 4.000.000 / mes')
  })
  it('el campo pone los puntos al escribir, descarta lo que no es cifra y limita a 13 cifras', () => {
    expect(textoDeCampo('3000000')).toBe('3.000.000')
    expect(textoDeCampo('Gs. 1.500.000a')).toBe('1.500.000')
    expect(textoDeCampo('000')).toBe('')
    expect(textoDeCampo('12345678901234567')).toBe('1.234.567.890.123')
    expect(montoDeCampo('3.000.000')).toBe(3000000); expect(montoDeCampo('')).toBeNull(); expect(montoDeCampo('0')).toBeNull()
  })
})

describe('Buscador: presupuesto "Desde" y "Hasta" en guaraníes', () => {
  it('pone los puntos al escribir, usa teclado numérico y no busca en cada tecla', async () => {
    app(); await screen.findByText('Departamento'); await waitFor(() => expect(busquedas().length).toBeGreaterThan(0))
    const antes = busquedas().length
    escribir(desde(), '3'); escribir(desde(), '300'); escribir(desde(), '300000000')
    expect(desde().value).toBe('300.000.000'); expect(desde().inputMode).toBe('numeric')
    await new Promise((r) => setTimeout(r, 50))
    expect(busquedas().length).toBe(antes)
  })
  it('salir del campo no busca; Buscar aplica "Desde", sin enviar moneda', async () => {
    app(); await screen.findByText('Departamento'); await waitFor(() => expect(busquedas().length).toBeGreaterThan(0))
    const antes = busquedas().length
    escribir(desde(), '300000000'); fireEvent.blur(desde())
    await new Promise((r) => setTimeout(r, 50)); expect(busquedas().length).toBe(antes)
    fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => expect(ultimaBusqueda()).toContain('presupuestoMin=300000000'))
    expect(ultimaBusqueda()).not.toContain('moneda=')
  })
  it('Enter aplica "Hasta"', async () => {
    app(); await screen.findByText('Departamento')
    escribir(hasta(), '500000000'); fireEvent.keyDown(hasta(), { key: 'Enter' })
    await waitFor(() => expect(ultimaBusqueda()).toContain('presupuestoMax=500000000'))
  })
  it('el botón Buscar aplica ambos campos', async () => {
    app(); await screen.findByText('Departamento')
    escribir(desde(), '100000000'); escribir(hasta(), '500000000'); fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => { const u = ultimaBusqueda(); expect(u).toContain('presupuestoMin=100000000'); expect(u).toContain('presupuestoMax=500000000') })
  })
  it('"Desde" mayor que "Hasta": error bajo el campo y no se consulta al backend', async () => {
    app(); await screen.findByText('Departamento'); await waitFor(() => expect(busquedas().length).toBeGreaterThan(0))
    const antes = busquedas().length
    escribir(desde(), '500000000'); escribir(hasta(), '100000000'); fireEvent.click(screen.getByText('Buscar'))
    expect(await screen.findByText('"Desde" no puede ser mayor que "Hasta".')).toBeTruthy()
    expect(desde().getAttribute('aria-invalid')).toBe('true')
    expect(busquedas().length).toBe(antes)
  })
  it('los errores del backend se muestran bajo cada campo, sin el aviso general', async () => {
    control.erroresBusqueda = { presupuestoMin: 'Debe ser mayor a 0', presupuestoMax: 'Máximo 9.999.999.999.999' }
    app(); await screen.findByText('Departamento')
    escribir(hasta(), '5000000'); fireEvent.keyDown(hasta(), { key: 'Enter' })
    expect(await screen.findByText('Máximo 9.999.999.999.999')).toBeTruthy()
    expect(document.getElementById('presupuesto-hasta-error')!.textContent).toBe('Máximo 9.999.999.999.999')
    expect(document.getElementById('presupuesto-desde-error')!.textContent).toBe('Debe ser mayor a 0')
    expect(screen.queryByRole('alert')).toBeNull()
  })
  it('sin resultados en el rango: aviso con el presupuesto en guaraníes', async () => {
    app(); await screen.findByText('Departamento')
    escribir(desde(), '1000000000'); escribir(hasta(), '2000000000'); fireEvent.click(screen.getByText('Buscar'))
    const aviso = await screen.findByRole('status')
    expect(aviso.textContent).toBe('No hay propiedades publicadas entre Gs. 1.000.000.000 y Gs. 2.000.000.000. Probá ampliar el presupuesto o quitar algún filtro.')
  })
})

describe('Precios en guaraníes en tarjetas, detalle, mapa y ofertas', () => {
  // Las tarjetas del barrio se quitaron el 19/10 (el barrio muestra sus marcadores en el mapa)
  it('detalle', async () => {
    app('/buscar?propiedad=prop-001'); await screen.findByText('Casa luminosa con patio.')
    expect(screen.getAllByText('Venta: Gs. 450.000.000').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Alquiler: Gs. 4.000.000 / mes').length).toBeGreaterThan(0)
    expect(document.body.textContent).not.toMatch(/USD|PYG/)
  })
  it('tooltip del mapa', async () => {
    const { MapCanvasReal } = (await import('../src/components/PropertyMap/MapCanvas')) as any
    const p = (id: string, tipoOperacion: string, precio: number) => ({ type: 'Feature', geometry: { type: 'Point', coordinates: [-54.6, -25.5] },
      properties: { id, titulo: `Casa ${id}`, categoria: 'Casa', tipoOperacion, precio, moneda: 'PYG' } })
    render(<MapCanvasReal ciudades={[]} barriosVisibles={[]} propiedadesVisibles={[p('a', 'Venta', 450000000), p('b', 'Alquiler', 4000000)]}
      showCiudades={false} showBarrios={false} showPropiedades barriosLayerKey="" selectedBarrioId={null} flyToBounds={null}
      onSelectCiudad={() => {}} onSelectBarrio={() => {}} />)
    const textos = screen.getAllByTestId('tooltip').map((t) => t.textContent)
    expect(textos).toEqual(['Casa a · Gs. 450.000.000', 'Casa b · Gs. 4.000.000 / mes'])
  })
  it('ofertas de la portada', async () => {
    render(<MemoryRouter><OffersSection /></MemoryRouter>)
    expect(await screen.findByText('Gs. 450.000.000')).toBeTruthy()
    expect(screen.getByText('Gs. 280.000.000')).toBeTruthy()
    expect(document.body.textContent).not.toMatch(/USD|PYG/)
    // La etiqueta de la tarjeta dice "Vivienda" para las casas (19/10) y "Terreno" para los terrenos
    expect(screen.getByText('Vivienda')).toBeTruthy(); expect(screen.getByText('Terreno')).toBeTruthy()
    expect(screen.queryByText('Casa')).toBeNull()
  })
})

describe('Chat: presupuesto escrito con sus palabras', () => {
  const abrir = async () => { fireEvent.click(screen.getByLabelText('Abrir asistente')); await screen.findByLabelText('Pregunta para el asistente') }
  const hastaPresupuesto = async () => {
    await abrir()
    fireEvent.click(screen.getByText('Comprar una propiedad'))
    fireEvent.click(await screen.findByText('Ciudad del Este'))
    return (await screen.findByLabelText('Tu presupuesto')) as HTMLInputElement
  }
  const posts = () => pedidos.filter((p) => p.url === '/api/asistente/consultas')
  const terminar = async () => {
    fireEvent.change(await screen.findByPlaceholderText('Tu nombre'), { target: { value: 'Marta' } })
    fireEvent.change(screen.getByPlaceholderText('Tu teléfono'), { target: { value: '0981 333 444' } })
    fireEvent.submit(screen.getByPlaceholderText('Tu teléfono').closest('form')!)
    await screen.findByText(/Podés dejarnos tus datos/)
  }

  it('pregunta el presupuesto sin botones de montos: solo el cuadro de texto y "Omitir"', async () => {
    chat(); const campo = await hastaPresupuesto()
    expect(screen.getByText(/¿Cuál es tu presupuesto aproximado\? Escribilo con tus palabras/)).toBeTruthy()
    expect(campo.placeholder).toBe('Escribí tu presupuesto…'); expect(campo.maxLength).toBe(150)
    expect(screen.getByText('Omitir')).toBeTruthy()
    expect(screen.queryByText(/USD/)).toBeNull(); expect(screen.queryByText(/Hasta Gs\./)).toBeNull()
  })
  it('lo escrito queda tal cual en el resumen y en WhatsApp, y no se envía al asistente de IA', async () => {
    control.sitio = { inicio: {}, servicios: {}, nosotros: {}, ofertas: [], contactoProvisional: false, contacto: CONTACTO_REAL }
    chat(); const campo = await hastaPresupuesto()
    fireEvent.change(campo, { target: { value: 'menos de 100.000.000 Gs.' } }); fireEvent.click(screen.getByText('Enviar'))
    expect(screen.getAllByText('menos de 100.000.000 Gs.').length).toBeGreaterThan(0)   // burbuja del cliente
    await terminar()
    const fila = screen.getByText('Presupuesto').closest('div')!
    expect(within(fila).getByText('menos de 100.000.000 Gs.')).toBeTruthy()
    const href = decodeURIComponent(screen.getByText(/Enviar por WhatsApp/).closest('a')!.getAttribute('href')!)
    expect(href).toContain('• Presupuesto: menos de 100.000.000 Gs.')
    expect(posts()).toHaveLength(0)
  })
  it('"Omitir" registra "No indicado"', async () => {
    chat(); await hastaPresupuesto()
    fireEvent.click(screen.getByText('Omitir'))
    expect(screen.getByText('Prefiero no indicarlo')).toBeTruthy()
    await terminar()
    expect(within(screen.getByText('Presupuesto').closest('div')!).getByText('No indicado')).toBeTruthy()
  })
  it('fuera del recorrido guiado, un mensaje con presupuesto va al asistente como pregunta', async () => {
    chat(); await abrir()
    fireEvent.change(screen.getByLabelText('Pregunta para el asistente'), { target: { value: 'Hola, quiero ver casas de menos de 100.000.000 gs' } })
    fireEvent.click(screen.getByText('Enviar'))
    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(posts()[0].cuerpo).toEqual({ pregunta: 'Hola, quiero ver casas de menos de 100.000.000 gs', codigoPropiedad: null })
  })
})

describe('Tipos de propiedad: solo Casa y Terreno (sin "departamento")', () => {
  it('el filtro ofrece Todos, Vivienda y Terreno', async () => {
    app(); await screen.findByText('Departamento')                      // el filtro territorial sigue igual
    const tipo = screen.getByText('Tipo de propiedad').parentElement!.querySelector('select')!
    expect([...tipo.options].map((o) => o.textContent)).toEqual(['Todos', 'Vivienda', 'Terreno'])
    expect(tipo.textContent).not.toMatch(/depto|departamento/i)
  })
  it('el chat pasa de "Comprar una propiedad" directo a la ciudad, sin preguntar casa o departamento', async () => {
    chat(); fireEvent.click(screen.getByLabelText('Abrir asistente')); await screen.findByLabelText('Pregunta para el asistente')
    fireEvent.click(screen.getByText('Comprar una propiedad'))
    expect(await screen.findByText('¿En qué ciudad te gustaría buscar?')).toBeTruthy()
    expect(screen.getByText('Ciudad del Este')).toBeTruthy()
    expect(document.body.textContent).not.toMatch(/departamento|Me da igual/i)
  })
})

describe('Detalle de la propiedad: título, foto, precio y "Detalles"', () => {
  it('el título va arriba de la foto con el botón de cerrar al lado; el precio y "Detalles" debajo', async () => {
    app('/buscar?propiedad=prop-001')
    const titulo = await screen.findByRole('heading', { name: 'Casa en Area 1' })
    // La X (hay otro "Volver a los resultados" con texto, sobre el mapa) va en la misma fila que el título
    expect(titulo.parentElement!.querySelector('button[aria-label="Volver a los resultados"]')).toBeTruthy()
    const etiqueta = screen.getByText('VENTA · ALQUILER')                       // va sobre la foto
    const precio = screen.getAllByText('Venta: Gs. 450.000.000').at(-1)!
    const detalles = screen.getByText('Detalles')
    const antes = (a: Node, b: Node) => Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)
    expect(antes(titulo, etiqueta)).toBe(true); expect(antes(etiqueta, precio)).toBe(true); expect(antes(precio, detalles)).toBe(true)
    expect(antes(detalles, screen.getByText('Dormitorios'))).toBe(true)
    expect(screen.queryByText(/Especificaciones de la propiedad/)).toBeNull()
  })
})

describe('Filtros: se aplican al pulsar Buscar', () => {
  const selectDe = (etiqueta: string) => screen.getByText(etiqueta).parentElement!.querySelector('select') as HTMLSelectElement
  it('elegir ciudad, tipo y operación no busca ni cambia el mapa; Buscar aplica todo junto', async () => {
    app(); await screen.findByText('Departamento'); await waitFor(() => expect(busquedas().length).toBeGreaterThan(0))
    const antes = busquedas().length
    fireEvent.change(selectDe('Ciudad'), { target: { value: '1' } })
    fireEvent.change(selectDe('Tipo de propiedad'), { target: { value: 'Vivienda' } })
    fireEvent.change(selectDe('Operación'), { target: { value: 'Venta' } })
    await new Promise((r) => setTimeout(r, 50))
    expect(busquedas().length).toBe(antes)                                    // nada se buscó todavía
    expect(selectDe('Barrio')).toBeTruthy()                                   // la lista de barrios ya se ajusta a la ciudad elegida
    expect([...selectDe('Barrio').options].map((o) => o.textContent)).toContain('AREA 1')
    fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => { const u = ultimaBusqueda(); expect(u).toContain('ciudadId=1'); expect(u).toContain('categoria=Vivienda'); expect(u).toContain('operacion=Venta') })
    expect(busquedas().length).toBe(antes + 1)                                // una sola búsqueda con todo
  })
  it('con Terreno elegido (sin buscar) ya no se ofrece Alquiler', async () => {
    app(); await screen.findByText('Departamento')
    fireEvent.change(selectDe('Operación'), { target: { value: 'Alquiler' } })
    fireEvent.change(selectDe('Tipo de propiedad'), { target: { value: 'Terreno' } })
    expect(selectDe('Operación').value).toBe('Todos')
    expect([...selectDe('Operación').options].map((o) => o.value)).not.toContain('Alquiler')
  })
  it('"Volver" en el mapa se aplica al instante y la barra lo refleja', async () => {
    app('/buscar?ciudad=1&barrio=10011013')
    await waitFor(() => expect(selectDe('Barrio').value).toBe('10011013'))
    const antes = busquedas().length
    fireEvent.click(screen.getAllByRole('button', { name: /Volver/ })[0])
    await waitFor(() => expect(busquedas().length).toBe(antes + 1))          // sin pulsar Buscar
    expect(ultimaBusqueda()).not.toContain('barrioId=')
    expect(selectDe('Barrio').value).toBe('')
  })
})

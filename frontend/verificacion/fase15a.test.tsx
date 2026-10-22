import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import SearchFilter from '../src/components/busqueda/SearchFilter'
import AssistantWidget from '../src/components/chat/AssistantWidget'
import { construirQuery } from '../src/services/api'
import { operacionesDe, tieneOperacion } from '../src/components/PropertyMap/types'
import { fetchFalso, pedidos, control } from './backendFalso'

vi.mock('../src/components/PropertyMap/MapCanvas', () => ({ default: () => null }))
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any

function app(ruta = '/buscar') {
  return render(<MemoryRouter initialEntries={[ruta]}><SearchFilter /><AssistantWidget /></MemoryRouter>)
}
const ultimaBusqueda = () => [...pedidos].reverse().find((p) => p.url.startsWith('/api/propiedades'))!.url
const seleccionar = (etiqueta: string, valor: string) =>
  fireEvent.change(screen.getByText(etiqueta).parentElement!.querySelector('select')!, { target: { value: valor } })

beforeEach(() => {
  reiniciarContenidoSitio(); cleanup(); pedidos.length = 0; control.caido = false; control.respuestaAsistente = null; vi.stubGlobal('fetch', vi.fn(fetchFalso)) })

describe('Funciones auxiliares', () => {
  it('construirQuery omite los valores vacíos y codifica', () => {
    expect(construirQuery({ a: 1, b: null, c: '', d: undefined, e: 'Área 1' })).toBe('?a=1&e=%C3%81rea%201')
    expect(construirQuery({})).toBe('')
  })
  it('operacionesDe usa "operaciones" o, si no viene, la operación principal', () => {
    const base = { tipoOperacion: 'Venta', precio: 1, moneda: 'PYG' } as any
    expect(operacionesDe(base)).toEqual([{ operacion: 'Venta', precio: 1, moneda: 'PYG' }])
    expect(tieneOperacion({ ...base, operaciones: [{ operacion: 'Alquiler', precio: 2, moneda: 'PYG' }] }, 'Alquiler')).toBe(true)
  })
})

describe('Buscador conectado a la API', () => {
  it('carga ciudades, barrios y propiedades desde /api (ya no desde /data)', async () => {
    app(); await screen.findByText('Departamento')
    const urls = pedidos.map((p) => p.url)
    expect(urls).toContain('/api/ciudades?conPropiedades=true'); expect(urls).toContain('/api/barrios?conPropiedades=true') // solo lo que tiene propiedades
    expect(urls.some((u) => u.startsWith('/api/propiedades'))).toBe(true)
    expect(urls.some((u) => u.startsWith('/data/'))).toBe(false)
  })
  it('cada filtro se envía al backend al pulsar Buscar, el presupuesto en guaraníes y sin moneda', async () => {
    app(); await screen.findByText('Departamento')
    seleccionar('Departamento', '1'); seleccionar('Ciudad', '1')
    seleccionar('Tipo de propiedad', 'Vivienda'); seleccionar('Operación', 'Alquiler')
    const hasta = screen.getByLabelText('Hasta (Gs.)'); fireEvent.change(hasta, { target: { value: '5000000' } })
    fireEvent.click(screen.getByText('Buscar'))
    await waitFor(() => { const u = ultimaBusqueda()
      expect(u).toContain('departamentoId=1'); expect(u).toContain('ciudadId=1')
      expect(u).toContain('categoria=Vivienda'); expect(u).toContain('operacion=Alquiler'); expect(u).toContain('presupuestoMax=5000000'); expect(u).not.toContain('moneda=') })
  })
  it('el combo de departamento lista los de la API y filtra las ciudades', async () => {
    app(); await screen.findByText('Departamento')
    expect(within(screen.getByText('Departamento').parentElement!.querySelector('select')!).getByText('ALTO PARANÁ')).toBeTruthy()
    expect(within(screen.getByText('Ciudad').parentElement!.querySelector('select')!).getByText('HERNANDARIAS')).toBeTruthy()
  })
  it('con Terreno no se ofrece Alquiler', async () => {
    app(); await screen.findByText('Departamento'); seleccionar('Tipo de propiedad', 'Terreno')
    expect(within(screen.getByText('Operación').parentElement!.querySelector('select')!).queryByText('Alquiler')).toBeNull()
  })
  // Desde el 19/10 el barrio no tiene panel con contadores ni tarjetas: el mapa se acerca al barrio y muestra sus
  // marcadores. Las dos operaciones de una casa se siguen viendo en el detalle (prueba siguiente).
  it('en el barrio, el buscador pide al backend solo ese barrio y no muestra el panel anterior', async () => {
    app('/buscar?ciudad=1&barrio=10011013')
    await waitFor(() => expect(ultimaBusqueda()).toContain('barrioId=10011013'))
    expect(document.querySelector('.pm-breadcrumb span')!.textContent).toBe('Alto Paraná › CIUDAD DEL ESTE › AREA 1')
    expect(screen.queryByText('1 en venta')).toBeNull(); expect(screen.queryByText('1 en alquiler')).toBeNull()
  })
  it('el detalle muestra ambas operaciones, la descripción y el botón Consultar', async () => {
    app('/buscar?propiedad=prop-001')
    expect(await screen.findByText('Casa luminosa con patio.')).toBeTruthy()
    expect(screen.getByText('VENTA · ALQUILER')).toBeTruthy(); expect(screen.getByText('Venta y Alquiler')).toBeTruthy()
    expect(screen.getByText(/Consultar sobre esta propiedad/)).toBeTruthy()
  })
  it('si el backend no responde, muestra un mensaje claro', async () => {
    control.caido = true; app(); expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeTruthy()
  })
})

describe('Asistente', () => {
  beforeEach(() => { control.sitio = null })
  const abrir = async () => { fireEvent.click(screen.getByLabelText('Abrir asistente')); return screen.findByLabelText('Pregunta para el asistente') }
  const preguntar = async (texto: string) => {
    const input = (await screen.findByLabelText('Pregunta para el asistente')) as HTMLInputElement
    fireEvent.change(input, { target: { value: texto } }); fireEvent.click(screen.getByText('Enviar'))
  }
  const ultimoPost = () => [...pedidos].reverse().find((p) => p.metodo === 'POST')!
  it('conserva los botones guiados y agrega el texto libre', async () => {
    app(); await abrir(); expect(screen.getByText('Comprar una propiedad')).toBeTruthy()
    fireEvent.click(screen.getByText('Alquilar una propiedad')); expect(await screen.findByText('Hernandarias')).toBeTruthy()
  })
  it('pregunta general: respuesta con saltos de línea y sin WhatsApp si no corresponde', async () => {
    control.respuestaAsistente = { status: 200, json: { respuesta: 'Disponibles:\n- prop-001: Casa', sugerirWhatsApp: false, propiedadesRelacionadas: ['prop-001'] } }
    app(); await abrir(); await preguntar('¿Qué casas tienen?')
    const burbuja = await screen.findByText(/Disponibles:/)
    expect(burbuja.textContent).toContain('\n- prop-001'); expect((burbuja as HTMLElement).style.whiteSpace).toBe('pre-line')
    expect(ultimoPost().cuerpo).toEqual({ pregunta: '¿Qué casas tienen?', codigoPropiedad: null })
    expect(screen.queryByText(/Hablar con un asesor por WhatsApp/)).toBeNull()
  })
  it('"Consultar" desde el detalle: el chat no muestra el código, pero lo envía al backend y al mensaje de WhatsApp del equipo', async () => {
    control.sitio = { inicio: {}, servicios: {}, nosotros: {}, ofertas: [], contactoProvisional: false,
      contacto: { nombreEmpresa: 'S&N', descripcion: '', direccion: '', ciudad: '', email: 'info@sn.com.py', telefono: '0981', whatsapp: '595981111222', horario: '' } }
    control.respuestaAsistente = { status: 200, json: { respuesta: 'No dispongo de ese dato.', sugerirWhatsApp: true, propiedadesRelacionadas: ['prop-001'] } }
    app('/buscar?propiedad=prop-001'); fireEvent.click(await screen.findByText(/Consultar sobre esta propiedad/))
    expect(await screen.findByText('Escribí tu consulta sobre "Casa en Area 1" y te respondo con los datos que tenemos.')).toBeTruthy()
    expect(document.body.textContent).not.toContain('(prop-001)')
    expect((screen.getByLabelText('Pregunta para el asistente') as HTMLInputElement).placeholder).toBe('Preguntá sobre esta propiedad…')
    await preguntar('¿Tiene cochera?'); await screen.findByText('No dispongo de ese dato.')
    expect(ultimoPost().cuerpo).toEqual({ pregunta: '¿Tiene cochera?', codigoPropiedad: 'prop-001' })
    const enlace = screen.getByText(/Hablar con un asesor por WhatsApp/).closest('a')!
    expect(enlace.getAttribute('href')).toContain('https://wa.me/595981111222?text=')             // número del panel
    expect(decodeURIComponent(enlace.getAttribute('href')!)).toContain('"Casa en Area 1" (prop-001)') // el equipo ve título y código
    expect(screen.getByText('Dejar mis datos para que me contacten')).toBeTruthy()
    fireEvent.click(screen.getByText('Preguntar en general')); await preguntar('Hola')
    await waitFor(() => expect(ultimoPost().cuerpo).toEqual({ pregunta: 'Hola', codigoPropiedad: null }))
  })
  it('límite de consultas (429): muestra el mensaje del backend y ofrece WhatsApp', async () => {
    control.respuestaAsistente = { status: 429, json: { error: { codigo: 'DEMASIADAS_CONSULTAS', mensaje: 'Realizó muchas consultas seguidas.' } } }
    app(); await abrir(); await preguntar('Hola')
    expect(await screen.findByText('Realizó muchas consultas seguidas.')).toBeTruthy()
    // Sin número real en el panel (datos provisionales) no hay WhatsApp, pero sí el formulario
    expect(screen.queryByText(/Hablar con un asesor por WhatsApp/)).toBeNull()
    expect(screen.getByText('Dejar mis datos para que me contacten')).toBeTruthy()
  })
  it('no envía preguntas vacías y limita a 500 caracteres', async () => {
    app(); const input = (await abrir()) as HTMLInputElement
    expect(input.maxLength).toBe(500); expect((screen.getByText('Enviar') as HTMLButtonElement).disabled).toBe(true)
  })
})

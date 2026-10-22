import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../src/App'
import { fetchFalso, pedidos, control } from './backendFalso'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'

vi.mock('../src/components/PropertyMap/MapCanvas', () => ({ default: () => null }))
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
;(globalThis as any).IntersectionObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any
window.scrollTo = (() => {}) as any

const CONTACTO_REAL = { nombreEmpresa: 'S&N', descripcion: '', direccion: '', ciudad: '', email: 'info@sn.com.py', telefono: '0981', whatsapp: '595981111222', horario: '' }
const sitio = (ruta: string) => render(<MemoryRouter initialEntries={[ruta]}><App /></MemoryRouter>)
const abrirChat = async () => { fireEvent.click(screen.getByLabelText('Abrir asistente')); return screen.findByLabelText('Pregunta para el asistente') }
const preguntar = async (t: string) => { fireEvent.change(await screen.findByLabelText('Pregunta para el asistente'), { target: { value: t } }); fireEvent.click(screen.getByText('Enviar')) }
const campoForm = (t: string) => within(screen.getByRole('form', { name: 'Dejar mis datos' })).getByText(t).closest('label')!.querySelector('input, textarea') as HTMLInputElement
const envio = () => pedidos.find((p) => p.url === '/api/clientes-potenciales')?.cuerpo as any

beforeEach(() => {
  cleanup(); reiniciarContenidoSitio(); pedidos.length = 0; control.caido = false; control.respuestaCliente = null; control.sitio = null
  control.respuestaAsistente = { status: 200, json: { respuesta: 'Un asesor puede ayudarte con eso.', sugerirWhatsApp: true, propiedadesRelacionadas: [] } }
  vi.stubGlobal('fetch', vi.fn(fetchFalso))
})

describe('Chat: saludo y contacto', () => {
  it('saludo nuevo al abrir el chat', async () => {
    sitio('/nosotros'); await abrirChat()
    expect(screen.getByText('¡Hola! Soy tu asistente virtual. ¿Con qué puedo ayudarte?')).toBeTruthy()
  })
  it('sugerencia de asesor con datos provisionales: sin WhatsApp; el formulario llega con la pregunta y se envía como Chat del sitio', async () => {
    sitio('/nosotros'); await abrirChat(); await preguntar('Quiero visitar una casa')
    await screen.findByText('Un asesor puede ayudarte con eso.')
    expect(screen.queryByText(/WhatsApp/)).toBeNull()
    fireEvent.click(screen.getByText('Dejar mis datos para que me contacten'))
    expect(campoForm('Mensaje').value).toBe('Quiero visitar una casa')            // completado de antemano, editable
    fireEvent.click(screen.getByText('Enviar mis datos'))
    expect(screen.getByText('Ingresá tu nombre')).toBeTruthy(); expect(envio()).toBeUndefined()
    fireEvent.change(campoForm('Nombre'), { target: { value: 'Luis' } })
    fireEvent.change(campoForm('Teléfono'), { target: { value: '0991 222 333' } })
    fireEvent.click(within(screen.getByRole('form', { name: 'Dejar mis datos' })).getByRole('checkbox'))
    fireEvent.click(screen.getByText('Enviar mis datos'))
    expect(await screen.findByText(/Un asesor se pondrá en contacto/)).toBeTruthy()
    expect(envio()).toMatchObject({ nombre: 'Luis', telefono: '0991 222 333', mensaje: 'Quiero visitar una casa', canal: 'CHAT', aceptaTratamiento: true })
  })
  it('desde una propiedad, el formulario envía su código (para vincularla en el panel)', async () => {
    sitio('/buscar?propiedad=prop-001'); fireEvent.click(await screen.findByText(/Consultar sobre esta propiedad/))
    await preguntar('¿Se puede financiar?'); await screen.findByText('Un asesor puede ayudarte con eso.')
    fireEvent.click(screen.getByText('Dejar mis datos para que me contacten'))
    fireEvent.change(campoForm('Nombre'), { target: { value: 'Ana' } }); fireEvent.change(campoForm('Email'), { target: { value: 'ana@correo.test' } })
    fireEvent.click(within(screen.getByRole('form', { name: 'Dejar mis datos' })).getByRole('checkbox'))
    fireEvent.click(screen.getByText('Enviar mis datos'))
    await screen.findByText(/Un asesor se pondrá en contacto/)
    expect(envio()).toMatchObject({ codigoPropiedad: 'prop-001', canal: 'CHAT', email: 'ana@correo.test', telefono: null })
  })
  it('con número real en el panel: WhatsApp y formulario', async () => {
    control.sitio = { inicio: {}, servicios: {}, nosotros: {}, ofertas: [], contactoProvisional: false, contacto: CONTACTO_REAL }
    sitio('/nosotros'); await abrirChat(); await preguntar('Hola')
    await screen.findByText('Un asesor puede ayudarte con eso.')
    expect(screen.getByText(/Hablar con un asesor por WhatsApp/).closest('a')!.getAttribute('href')).toContain('https://wa.me/595981111222')
    expect(screen.getByText('Dejar mis datos para que me contacten')).toBeTruthy()
  })
  it('si el asistente no puede responder, igual se ofrece el formulario', async () => {
    control.respuestaAsistente = { status: 503, json: { error: { codigo: 'ASISTENTE_NO_DISPONIBLE', mensaje: 'El asistente no puede responder en este momento.' } } }
    sitio('/nosotros'); await abrirChat(); await preguntar('¿Tienen terrenos?')
    await screen.findByText('El asistente no puede responder en este momento.')
    fireEvent.click(screen.getByText('Dejar mis datos para que me contacten'))
    expect(campoForm('Mensaje').value).toBe('¿Tienen terrenos?')
  })
})

describe('Chat: flujo guiado', () => {
  it('el resumen ofrece el formulario completado con nombre, teléfono y la búsqueda', async () => {
    sitio('/nosotros'); await abrirChat()
    fireEvent.click(screen.getByText('Comprar una propiedad'))
    const zona = (await screen.findAllByRole('button')).find((b) => /CIUDAD DEL ESTE|Ciudad del Este/i.test(b.textContent ?? ''))!
    fireEvent.click(zona)
    const presupuesto = await screen.findByLabelText('Tu presupuesto')         // W4: lo escribe con sus palabras
    fireEvent.change(presupuesto, { target: { value: 'entre 300 y 500 millones de Gs.' } }); fireEvent.click(screen.getByText('Enviar'))
    fireEvent.change(await screen.findByPlaceholderText('Tu nombre'), { target: { value: 'Marta' } })
    fireEvent.change(screen.getByPlaceholderText('Tu teléfono'), { target: { value: '0981 333 444' } })
    fireEvent.submit(screen.getByPlaceholderText('Tu teléfono').closest('form')!)
    expect(await screen.findByText(/Podés dejarnos tus datos/)).toBeTruthy()
    expect(screen.queryByText('Enviar por WhatsApp')).toBeNull()                 // datos provisionales: sin WhatsApp
    fireEvent.click(screen.getByText('Dejar mis datos para que me contacten'))
    expect(campoForm('Nombre').value).toBe('Marta'); expect(campoForm('Teléfono').value).toBe('0981 333 444')
    expect(campoForm('Mensaje').value).toContain('• Presupuesto: entre 300 y 500 millones de Gs.')
  })
})

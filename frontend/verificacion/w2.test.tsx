import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../src/App'
import { fetchFalso, pedidos, control } from './backendFalso'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import { desplazamientos } from './preparar'

vi.mock('../src/components/PropertyMap/MapCanvas', () => ({ default: () => null }))
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
;(globalThis as any).IntersectionObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any
window.scrollTo = (() => {}) as any

/** Contenido del panel (como GET /api/sitio), distinto del respaldo para comprobar que se usa. */
const SITIO = {
  inicio: { imagenPortada: '/uploads/sitio-001/0a4c5cd8-14cc-4bbc-a4d8-0f7bc29c541d.png', titulo: 'Tu hogar *en Alto Paraná*', subtitulo: 'Intro del panel' },
  servicios: { titulo: 'Lo que *hacemos*', subtitulo: 'Sub del panel', servicios: [
    { titulo: 'Tasaciones', texto: 'Valuamos.' }, { titulo: 'Alquileres', texto: 'Rápido.' }, { titulo: 'Terrenos', texto: 'Lotes.' },
    { titulo: 'Asesoría', texto: 'Te guiamos.' }, { titulo: 'Gestoría', texto: 'Papeles.' }], bloqueTitulo: 'Bloque *del panel*', bloqueTexto: 'Texto del bloque' },
  nosotros: { introduccion: 'Intro nosotros', historiaTitulo: 'Historia *propia*', historiaTexto: 'Nuestra historia.', imagen: null,
    cifras: [{ valor: '+30', etiqueta: 'Propiedades' }], valores: [{ titulo: 'Cercanía', texto: 'Trato directo.' }],
    razones: [{ titulo: 'Rapidez', texto: 'Respondemos pronto.' }, { titulo: 'Claridad', texto: 'Sin letra chica.' }] },
  contacto: { nombreEmpresa: 'S&N Inmobiliaria', descripcion: 'Descripción del panel', direccion: 'Av. Monseñor Rodríguez 123', ciudad: 'Ciudad del Este',
    email: 'info@sn.com.py', telefono: '+595 61 500 000', whatsapp: '595981111222', horario: 'Lunes a viernes de 8:00 a 18:00' },
  contactoProvisional: false,
  ofertas: ['terr-001', 'prop-001'],
}
const sitio = (ruta: string) => render(<MemoryRouter initialEntries={[ruta]}><App /></MemoryRouter>)
const campo = (t: string | RegExp) => screen.getByText(t).closest('label')!.querySelector('input, textarea, select') as HTMLInputElement

beforeEach(() => {
  cleanup(); reiniciarContenidoSitio(); pedidos.length = 0; control.caido = false; control.respuestaCliente = null
  control.sitio = JSON.parse(JSON.stringify(SITIO)); vi.stubGlobal('fetch', vi.fn(fetchFalso))
})

describe('Contenido del panel en el sitio', () => {
  it('portada: título con palabras destacadas, introducción e imagen; sin el texto de 12.000', async () => {
    const { container } = sitio('/')
    const em = await screen.findByText('en Alto Paraná')
    expect(em.tagName).toBe('EM'); expect(em.closest('h1')!.textContent).toBe('Tu hogar en Alto Paraná') // sin asteriscos visibles
    expect(screen.getByText('Intro del panel')).toBeTruthy()
    expect(screen.queryByText(/12\.000/)).toBeNull()
    expect((container.querySelector('[style*="background-image"]') as HTMLElement).style.backgroundImage).toContain('/uploads/sitio-001/')
  })
  it('ofertas: las elegidas en el panel, en su orden', async () => {
    const { container } = sitio('/')
    await waitFor(() => expect(container.querySelectorAll('[class*=cardTitle]').length).toBe(2))
    expect([...container.querySelectorAll('[class*=cardTitle]')].map((e) => e.textContent)).toEqual(['Terreno en Centro', 'Casa en Area 1'])
  })
  it('servicios: /servicios lleva a Nosotros, a los servicios del panel (5), con fotos asignadas por posición', async () => {
    desplazamientos.length = 0
    const { container } = sitio('/servicios')
    expect(await screen.findByText('Gestoría')).toBeTruthy()
    expect(screen.getByText('Intro nosotros')).toBeTruthy()                       // es la página Nosotros
    await waitFor(() => expect(desplazamientos).toContain('servicios'))            // y baja hasta los servicios
    const tarjetas = [...container.querySelectorAll('[class*=servicioCard]')] as HTMLElement[]
    expect(tarjetas).toHaveLength(5)
    expect(tarjetas[4].style.backgroundImage).toBe(tarjetas[0].style.backgroundImage) // la quinta repite la foto de la primera
    expect(screen.getByText('hacemos').tagName).toBe('EM')                        // título de los servicios, con su destacado
    expect(screen.getByText('Sub del panel')).toBeTruthy()
    // Se retiraron "Lo que tenemos para vos" y su carrusel
    expect(screen.queryByText(/Lo que tenemos para vos|Bloque|Texto del bloque/)).toBeNull()
    expect(container.querySelector('[class*=carousel]')).toBeNull()
  })
  it('nosotros: introducción, historia, cifras, valores y servicios del panel, en ese orden; sin "Por qué elegirnos"', async () => {
    const { container } = sitio('/nosotros')
    expect(await screen.findByText('Intro nosotros')).toBeTruthy()
    for (const t of ['Nuestra historia.', '+30', 'Cercanía', 'Tasaciones']) expect(screen.getByText(t)).toBeTruthy()
    expect(screen.queryByText('+150')).toBeNull()
    for (const t of ['Rapidez', 'Claridad', /Por qué elegir/]) expect(screen.queryByText(t)).toBeNull()
    expect(container.querySelector('img[src*="nosotros_page2"]')).toBeNull()       // su imagen también se retiró
    const antes = (a: Node, b: Node) => Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)
    expect(antes(screen.getByText('Nuestra historia.'), screen.getByText('Cercanía'))).toBe(true)
    expect(antes(screen.getByText('Cercanía'), screen.getByText('Tasaciones'))).toBe(true)
  })
  it('el menú y el pie ya no tienen "Servicios"', async () => {
    sitio('/nosotros'); await screen.findByText('Intro nosotros')
    expect(screen.queryAllByRole('link', { name: 'Servicios' })).toHaveLength(0)
    expect(screen.queryAllByRole('button', { name: 'Servicios' })).toHaveLength(0)
    expect(screen.queryByText('Servicios', { selector: 'nav *, footer *' })).toBeNull()
  })
  it('nosotros: sin los subtítulos redundantes "Quiénes somos", "Nuestros valores" y "Por qué elegirnos"', async () => {
    sitio('/nosotros')
    await screen.findByText('Intro nosotros')
    for (const t of [/Quiénes somos/i, /Nuestros valores/i, /Por qué elegirnos/i]) expect(screen.queryByText(t)).toBeNull()
    expect(screen.getByText('Nuestra historia.')).toBeTruthy()                 // los títulos principales siguen
    expect(screen.getByText(/guía todos los días/)).toBeTruthy()
  })
  it('pie de página: descripción y ciudad del panel', async () => {
    sitio('/nosotros')
    expect(await screen.findByText('Descripción del panel')).toBeTruthy()
    expect(screen.getByText('Ciudad del Este, Paraguay')).toBeTruthy()
  })
  it('si el backend no responde, se muestran los textos de respaldo (nunca vacío)', async () => {
    control.sitio = null
    sitio('/')
    expect(await screen.findByText('tu próxima historia.')).toBeTruthy()
  })
})

describe('Página Contacto', () => {
  it('mensaje institucional y canales con enlaces mailto, tel y WhatsApp', async () => {
    sitio('/contacto')
    expect(await screen.findByText('info@sn.com.py')).toBeTruthy()
    expect(screen.getByText('Tu consulta nos interesa, ponte en contacto con nosotros')).toBeTruthy()
    expect(screen.getByText('info@sn.com.py').closest('a')!.getAttribute('href')).toBe('mailto:info@sn.com.py')
    expect(screen.getByText('+595 61 500 000').closest('a')!.getAttribute('href')).toBe('tel:+59561500000')
    expect(screen.getByText('+595981111222').closest('a')!.getAttribute('href')).toBe('https://wa.me/595981111222')
    expect(screen.getByText('Av. Monseñor Rodríguez 123, Ciudad del Este')).toBeTruthy()
    expect(screen.queryByText(/Sonia Giménez|Nuestros Agentes/)).toBeNull()       // los agentes de ejemplo se retiraron
  })
  it('con datos provisionales no muestra enlaces de prueba', async () => {
    control.sitio = { ...SITIO, contacto: { ...SITIO.contacto, email: 'contacto@sn-inmobiliaria.example' }, contactoProvisional: true }
    sitio('/contacto')
    expect(await screen.findByText(/Datos de contacto en actualización/)).toBeTruthy()
    expect(screen.queryByText(/sn-inmobiliaria\.example/)).toBeNull()
    expect(document.querySelector('a[href^="mailto:"], a[href^="https://wa.me"]')).toBeNull()
  })
  it('formulario: valida antes de enviar y envía con canal CONTACTO y el asunto al inicio del mensaje', async () => {
    sitio('/contacto'); await screen.findByText('info@sn.com.py')
    fireEvent.click(screen.getByText('Enviar mensaje'))
    expect(screen.getByText('Ingresá tu nombre')).toBeTruthy()
    expect(screen.getByText(/Indicá un teléfono o un email/)).toBeTruthy()
    expect(screen.getByText(/tenés que aceptar el uso de tus datos/)).toBeTruthy()
    expect(pedidos.some((p) => p.url === '/api/clientes-potenciales')).toBe(false)
    fireEvent.change(campo('Nombre y apellido'), { target: { value: 'Ana Pérez' } })
    fireEvent.change(campo('Email'), { target: { value: 'ana@correo.test' } })
    fireEvent.change(campo('Asunto (opcional)'), { target: { value: 'Alquilar' } })
    fireEvent.change(campo('Mensaje'), { target: { value: 'Busco casa' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(screen.getByText('Enviar mensaje'))
    expect(await screen.findByText(/Un asesor se pondrá en contacto/)).toBeTruthy()
    const envio = pedidos.find((p) => p.url === '/api/clientes-potenciales')!.cuerpo as any
    expect(envio).toMatchObject({ nombre: 'Ana Pérez', email: 'ana@correo.test', telefono: null, canal: 'CONTACTO', aceptaTratamiento: true, versionConsentimiento: '2026-10' })
    expect(envio.mensaje).toBe('Asunto: Alquilar\nBusco casa')
    expect(envio).not.toHaveProperty('cedula')
  })
  it('los errores del backend aparecen junto a cada campo; el límite de envíos como mensaje general', async () => {
    control.respuestaCliente = { status: 400, json: { error: { codigo: 'VALIDACION', mensaje: 'Hay datos inválidos.', campos: { email: 'Ingrese un email válido' } } } }
    sitio('/contacto'); await screen.findByText('info@sn.com.py')
    fireEvent.change(campo('Nombre y apellido'), { target: { value: 'Ana' } }); fireEvent.change(campo('Email'), { target: { value: 'x@y' } })
    fireEvent.click(screen.getByRole('checkbox')); fireEvent.click(screen.getByText('Enviar mensaje'))
    expect(await screen.findByText('Ingrese un email válido')).toBeTruthy()
    control.respuestaCliente = { status: 429, json: { error: { codigo: 'DEMASIADAS_CONSULTAS', mensaje: 'Recibimos varios envíos seguidos.' } } }
    fireEvent.click(screen.getByText('Enviar mensaje'))
    expect(await screen.findByText('Recibimos varios envíos seguidos.')).toBeTruthy()
  })
})

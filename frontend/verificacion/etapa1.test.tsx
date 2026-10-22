import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, cleanup, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import App from '../src/App'
import { fetchFalso, pedidos, control } from './backendFalso'

vi.mock('../src/components/PropertyMap/MapCanvas', () => ({ default: () => null }))
;(globalThis as any).ResizeObserver = class { observe() {} unobserve() {} disconnect() {} }
;(globalThis as any).IntersectionObserver = class { observe() {} unobserve() {} disconnect() {} }
Element.prototype.scrollTo = function () {} as any
window.scrollTo = (() => {}) as any

const EMOJI = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B50}\u{2190}-\u{21FF}\u{2715}]/u
const sitio = (ruta: string) => render(<MemoryRouter initialEntries={[ruta]}><App /></MemoryRouter>)

beforeEach(() => {
  reiniciarContenidoSitio(); cleanup(); pedidos.length = 0; control.caido = false; control.respuestaAsistente = null; vi.stubGlobal('fetch', vi.fn(fetchFalso)) })

describe('Logo', () => {
  it('encabezado y pie muestran "S&N" + "Inmobiliaria", sin duplicar las letras', async () => {
    const { container } = sitio('/nosotros')
    const nav = container.querySelector('nav')!
    const pie = container.querySelector('footer')!
    for (const zona of [nav, pie]) {
      const texto = zona.textContent!.replace(/\s+/g, ' ')
      expect(texto).toMatch(/S&N\s*Inmobiliaria/)
      expect(texto).not.toMatch(/S&N\s*S&N/)
    }
    // En el encabezado, "S&N" aparece una sola vez (en la insignia)
    expect((nav.textContent!.match(/S&N/g) ?? []).length).toBe(1)
  })
})

describe('Sin emojis y con íconos coherentes', () => {
  for (const ruta of ['/', '/nosotros', '/servicios', '/contacto', '/buscar?ciudad=1&barrio=10011013', '/buscar?propiedad=prop-001']) {
    it(`la página ${ruta} no muestra emojis`, async () => {
      const { container } = sitio(ruta)
      // Nosotros y Contacto son estáticas; las demás esperan la respuesta de la API
      await new Promise((r) => setTimeout(r, 60))
      expect(container.textContent).not.toMatch(EMOJI)
      expect(container.querySelectorAll('svg.lucide').length).toBeGreaterThan(0)
    })
  }

  it('el chat abierto no muestra emojis y usa íconos', async () => {
    const { container } = sitio('/nosotros')
    expect(screen.getByLabelText('Abrir asistente').querySelector('svg.lucide')).toBeTruthy()
    fireEvent.click(screen.getByLabelText('Abrir asistente'))
    await screen.findByLabelText('Pregunta para el asistente')
    expect(container.textContent).not.toMatch(EMOJI)
    expect(screen.getByText('¡Hola! Soy tu asistente virtual. ¿Con qué puedo ayudarte?')).toBeTruthy()
  })

  // Desde el 19/10 el barrio no tiene panel con tarjetas: las tarjetas que quedan son las ofertas de la portada
  it('las tarjetas usan íconos de dormitorios, baños y superficie', async () => {
    const { container } = sitio('/')
    await screen.findByText('Gs. 450.000.000')
    for (const icono of ['lucide-bed-double', 'lucide-bath', 'lucide-ruler']) {
      expect(container.querySelector(`svg.${icono}`)).toBeTruthy()
    }
  })
})

describe('Favoritos', () => {
  it('las tarjetas ya no tienen el botón de favoritos (no guardaba nada)', async () => {
    sitio('/')
    await screen.findByText('Gs. 450.000.000')
    expect(screen.queryByLabelText(/favoritos/i)).toBeNull()
  })
})

describe('Funciones que deben seguir igual', () => {
  it('el detalle conserva cerrar, volver y "Consultar sobre esta propiedad"', async () => {
    sitio('/buscar?propiedad=prop-001')
    expect(await screen.findByText(/Consultar sobre esta propiedad/)).toBeTruthy()
    expect(screen.getByLabelText('Volver a los resultados')).toBeTruthy()
    expect(screen.getAllByText(/Volver a los resultados/).length).toBeGreaterThan(0)
  })
  it('Inicio conserva las ofertas y ya no tiene el bloque "Sobre nosotros"', async () => {
    sitio('/')
    expect(await screen.findByText('Ofertas destacadas')).toBeTruthy()
    expect(screen.queryByText(/Conocé más sobre nosotros/)).toBeNull()
  })
})

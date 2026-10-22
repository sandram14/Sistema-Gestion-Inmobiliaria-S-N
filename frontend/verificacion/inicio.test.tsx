import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, cleanup, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { reiniciarContenidoSitio } from '../src/services/contenidoSitio'
import OffersSection from '../src/components/home/OffersSection'
const fc = (features: unknown[]) => ({ type: 'FeatureCollection', features })
const props = Array.from({ length: 8 }, (_, i) => ({ type: 'Feature', geometry: { type: 'Point', coordinates: [-54.6, -25.5] },
  properties: { id: `prop-00${i + 1}`, titulo: `Casa ${i + 1}`, categoria: 'Casa', tipoOperacion: 'Venta', ciudad: 'CIUDAD DEL ESTE', barrio: 'AREA 1', barrioId: 10011013,
    precio: 1000, moneda: 'PYG', dormitorios: 2, banos: 1, m2: 80, direccion: '', fotos: [`/uploads/prop-00${i + 1}/0a4c5cd8-14cc-4bbc-a4d8-0f7bc29c541d.png`] } }))
beforeEach(() => {
  reiniciarContenidoSitio(); cleanup(); vi.stubGlobal('fetch', vi.fn(async (url: string) => new Response(JSON.stringify(String(url).includes('/api/propiedades') ? fc(props) : fc([])), { status: 200 }))) })
describe('Inicio sin historias de éxito ni bloque Sobre nosotros', () => {
  it('las ofertas usan fotos de /uploads y no queda nada de SuccessStories', async () => {
    const { container } = render(<MemoryRouter><OffersSection /></MemoryRouter>)
    await waitFor(() => expect(container.querySelectorAll('img[src^="/uploads/"]').length).toBe(6))
    const srcs = [...container.querySelectorAll('img')].map((i) => i.getAttribute('src') ?? '')
    expect(srcs.some((s) => s.includes('/images/propiedades/') || s.includes('/images/terrenos/'))).toBe(false)
    expect(srcs.some((s) => s.includes('unsplash'))).toBe(false)
    expect(screen.queryByText(/Martina Rodríguez/)).toBeNull()
    expect(screen.queryByText(/Conocé más sobre nosotros/)).toBeNull()   // el bloque "Sobre nosotros" se retiró
    expect(srcs).not.toContain('/images/stock/inicio_sobre_nosotros.jpg')
  })
})

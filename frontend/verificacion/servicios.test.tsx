import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, cleanup, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ServiciosSeccion from '../src/components/nosotros/ServiciosSeccion'
import { CONTENIDO_RESPALDO, reiniciarContenidoSitio } from '../src/services/contenidoSitio'

// Servicios dentro de Nosotros (antes era la página /servicios, con un carrusel de fotos que se retiró)
beforeEach(() => { cleanup(); reiniciarContenidoSitio() })
const montar = () => render(<MemoryRouter><ServiciosSeccion /></MemoryRouter>)

describe('Sección Servicios (en Nosotros)', () => {
  it('si el backend no responde, muestra los servicios de respaldo (nunca vacía)', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    const { container } = montar(); await new Promise((r) => setTimeout(r, 50))
    expect(container.querySelector('section#servicios')).toBeTruthy()              // ancla para /servicios
    expect(container.querySelectorAll('[class*=servicioCard]')).toHaveLength(CONTENIDO_RESPALDO.servicios.servicios.length)
    expect(screen.getByText(CONTENIDO_RESPALDO.servicios.servicios[0].titulo)).toBeTruthy()
  })
  it('no tiene carrusel ni el bloque "Lo que tenemos para vos"', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    const { container } = montar(); await new Promise((r) => setTimeout(r, 50))
    expect(container.querySelector('img')).toBeNull()
    expect(container.textContent).not.toMatch(/Lo que tenemos para vos|ideal para tu proyecto/)
  })
})

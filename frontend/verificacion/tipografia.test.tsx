import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'

const leer = (f: string) => readFileSync(f, 'utf-8')
/** Peso de la regla `selector { ... }` de un archivo CSS (la regla debe ser única). */
function peso(archivo: string, selector: string) {
  const reglas = [...leer(archivo).matchAll(new RegExp(`(?:^|\\n)${selector.replace('.', '\\.')} \\{([^}]*)\\}`, 'g'))]
  expect(reglas.length).toBe(1)
  return reglas[0][1].match(/font-weight:\s*(\d+);/)?.[1]
}

describe('Tipografía del sitio: Roboto, con los títulos importantes en negrita', () => {
  it('index.html carga Roboto desde Google Fonts con los enlaces indicados', () => {
    const html = leer('index.html')
    expect(html).toContain('<link rel="preconnect" href="https://fonts.googleapis.com">')
    expect(html).toContain('<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>')
    expect(html).toContain('<link href="https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100..900;1,100..900&display=swap" rel="stylesheet">')
  })
  it('las dos variables de fuente usan Roboto y ya no se cargan Montserrat ni Inter', () => {
    const css = leer('src/index.css')
    expect(css).toMatch(/--fuente-titulos: 'Roboto',/)
    expect(css).toMatch(/--fuente-texto: 'Roboto',/)
    expect(css).not.toMatch(/Montserrat|'Inter'|fonts\.googleapis/)
  })
  it('los títulos importantes van en 700', () => {
    const titulos: [string, string[]][] = [
      ['src/components/home/HeroSection.module.css', ['.headline']],
      ['src/components/home/OffersSection.module.css', ['.title']],
      ['src/components/busqueda/SearchFilter.module.css', ['.detailTitle']],
      ['src/components/nosotros/Nosotros.module.css', ['.title', '.h2', '.valorTitle', '.ctaTitle']],
      ['src/components/nosotros/ServiciosSeccion.module.css', ['.h2', '.servicioTitle']],
      ['src/components/contacto/Contacto.module.css', ['.title']],
    ]
    for (const [archivo, selectores] of titulos)
      for (const s of selectores) expect(`${s}: ${peso(archivo, s)}`).toBe(`${s}: 700`)
  })
})

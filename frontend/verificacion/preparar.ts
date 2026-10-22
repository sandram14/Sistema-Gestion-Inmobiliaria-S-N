// Preparación común de las pruebas (jsdom): funciones del navegador que jsdom no implementa.
// scrollIntoView registra a qué elemento se pidió desplazarse (lo usa la prueba de /servicios → Nosotros).
export const desplazamientos: string[] = []
Element.prototype.scrollIntoView = function (this: Element) { desplazamientos.push(this.id || this.tagName) }

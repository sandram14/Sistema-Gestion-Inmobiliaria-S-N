// Montos en guaraníes, la única moneda del sistema (migración 009).
// El formato es siempre el paraguayo, con punto como separador de miles (4.000.000),
// sin depender del idioma del navegador.

/** Máximo de cifras de un monto: el precio se guarda como numeric(15, 2), es decir, 13 cifras enteras. */
export const MAX_CIFRAS = 13

/** 4000000 -> "4.000.000" */
export function conPuntosDeMiles(monto: number): string {
  return String(Math.round(monto)).replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

/** 4000000 -> "Gs. 4.000.000" */
export function textoGuaranies(monto: number): string {
  return `Gs. ${conPuntosDeMiles(monto)}`
}

/** Precio de una operación: "Gs. 450.000.000" o "Gs. 4.000.000 / mes" (alquiler). */
export function textoPrecioOperacion(operacion: string, precio: number): string {
  return `${textoGuaranies(precio)}${operacion === 'Alquiler' ? ' / mes' : ''}`
}

/**
 * Lo que el visitante escribe en un campo de monto, con los puntos de miles puestos a medida que
 * escribe: "3000000" -> "3.000.000". Se descarta todo lo que no sea una cifra.
 */
export function textoDeCampo(escrito: string): string {
  const cifras = escrito.replace(/\D/g, '').replace(/^0+/, '').slice(0, MAX_CIFRAS)
  return cifras ? conPuntosDeMiles(Number(cifras)) : ''
}

/** "3.000.000" -> 3000000; vacío o cero -> null (sin límite). */
export function montoDeCampo(texto: string): number | null {
  const cifras = texto.replace(/\D/g, '')
  const monto = cifras ? Number(cifras) : 0
  return monto > 0 ? monto : null
}

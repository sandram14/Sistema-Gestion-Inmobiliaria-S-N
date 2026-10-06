import type { OperacionPrecio } from '../api/tipos'

/** "Venta USD 45.000" / "Alquiler PYG 4.000.000 / mes" */
export function textoOperacion(op: OperacionPrecio): string {
  return `${op.operacion} ${op.moneda} ${op.precio.toLocaleString('es-PY')}${op.operacion === 'Alquiler' ? ' / mes' : ''}`
}

export function textoFecha(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('es-PY', { dateStyle: 'short', timeStyle: 'short' })
}

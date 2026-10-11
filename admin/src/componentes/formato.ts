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

/** Nombre del rol para mostrar ("ADMIN" -> "Administrador"). */
export function nombreDelRol(rol: string | null | undefined): string {
  const nombres: Record<string, string> = { ADMIN: 'Administrador', GERENTE: 'Gerente', VENDEDOR: 'Vendedor' }
  return rol ? nombres[rol] ?? rol : ''
}

import type { OperacionPrecio } from '../api/tipos'

/** Monto en guaraníes con puntos de miles: 4000000 -> "Gs. 4.000.000". */
export function textoGuaranies(monto: number): string {
  return `Gs. ${Math.round(monto).toLocaleString('es-PY')}`
}

/** "Venta Gs. 450.000.000" / "Alquiler Gs. 4.000.000 / mes" */
export function textoOperacion(op: OperacionPrecio): string {
  return `${op.operacion} ${textoGuaranies(op.precio)}${op.operacion === 'Alquiler' ? ' / mes' : ''}`
}

export function textoFecha(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('es-PY', { dateStyle: 'short', timeStyle: 'short' })
}

/**
 * Nombre del tipo de propiedad para mostrar: en la base el tipo se llama "Casa", pero el panel
 * (como el sitio) lo muestra como "Vivienda". El valor que se envía sigue siendo "Casa".
 */
export function nombreDelTipo(tipo: string): string {
  return tipo === 'Casa' ? 'Vivienda' : tipo
}

/** Nombre del rol para mostrar ("ADMIN" -> "Administrador"). */
export function nombreDelRol(rol: string | null | undefined): string {
  const nombres: Record<string, string> = { ADMIN: 'Administrador', GERENTE: 'Gerente', VENDEDOR: 'Vendedor' }
  return rol ? nombres[rol] ?? rol : ''
}

import { ApiError } from '../api/cliente'

/** Errores de un formulario: por campo (400 con "campos") y un mensaje general. */
export function erroresDe(e: unknown): { campos: Record<string, string>; general: string } {
  if (e instanceof ApiError) return { campos: e.campos, general: e.message }
  return { campos: {}, general: 'No se pudo completar la operación.' }
}

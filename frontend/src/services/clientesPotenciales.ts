/**
 * Envío de clientes potenciales (formulario de Contacto y, en la W3, el del chat).
 * Contrato: docs/integracion-clientes-potenciales.md
 */
import { apiPost } from './api'

/** Versión del texto de aceptación; debe coincidir con la del backend (ClientePotencialService). */
export const VERSION_CONSENTIMIENTO = '2026-10'

export const TEXTO_ACEPTACION =
  'Acepto que S&N Inmobiliaria use mis datos (nombre y medio de contacto) únicamente para contactarme sobre esta consulta. Mis datos no se comparten con terceros.'

export interface DatosContacto {
  nombre: string
  telefono: string
  email: string
  mensaje: string
  aceptaTratamiento: boolean
  codigoPropiedad?: string | null
}

/** Envía la solicitud. Los errores (400 con campos, 429, 503) llegan como ApiError. */
export function enviarClientePotencial(d: DatosContacto, canal: 'CHAT' | 'CONTACTO'): Promise<{ registrado: boolean; mensaje: string }> {
  return apiPost('/api/clientes-potenciales', {
    nombre: d.nombre.trim(),
    telefono: d.telefono.trim() || null,
    email: d.email.trim() || null,
    mensaje: d.mensaje.trim() || null,
    codigoPropiedad: d.codigoPropiedad ?? null,
    aceptaTratamiento: d.aceptaTratamiento,
    versionConsentimiento: VERSION_CONSENTIMIENTO,
    canal,
  })
}

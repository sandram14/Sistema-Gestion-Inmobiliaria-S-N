import type { EstadoCliente, OrigenCliente } from '../api/tipos'
import ui from '../ui.module.css'

export const NOMBRE_ESTADO: Record<EstadoCliente, string> = { PENDIENTE: 'Pendiente de contacto', CONTACTADO: 'Contactado', DESCARTADO: 'Descartado' }
export const NOMBRE_ORIGEN: Record<OrigenCliente, string> = { CHAT: 'Chat del sitio', WHATSAPP: 'WhatsApp', LLAMADA: 'Llamada', OTRO: 'Otro' }

export function EstadoCli({ valor }: { valor: EstadoCliente }) {
  const clase = valor === 'PENDIENTE' ? ui.estadoPendiente : valor === 'CONTACTADO' ? ui.estado : `${ui.estado} ${ui.estadoInactivo}`
  return <span className={clase}>{NOMBRE_ESTADO[valor]}</span>
}

import type { ResultadoConsulta } from '../api/tipos'
import ui from '../ui.module.css'

export const NOMBRE_RESULTADO: Record<ResultadoConsulta, string> = {
  RESPONDIDA: 'Respondida', DERIVADA_ASESOR: 'Derivada a un asesor', SIN_SERVICIO: 'Sin servicio', BLOQUEADA: 'Bloqueada',
  FUERA_DE_AMBITO: 'Fuera del ámbito',
}

export const NOTA_REVISION =
  'Revisión interna: indica si alguien del equipo ya leyó la conversación. No significa que el cliente esté esperando una respuesta ni que haya sido atendido por una persona.'

/** Texto honesto para consultas anteriores al registro: no se deduce nada. */
export const NO_REGISTRADO = 'No registrado (consulta anterior al registro de resultados y recomendaciones)'

export function Resultado({ valor }: { valor: ResultadoConsulta | null }) {
  if (!valor) return <span className={ui.secundario}>No registrado</span>
  return <span className={`${ui.estado} ${valor === 'RESPONDIDA' ? '' : ui.estadoInactivo}`}>{NOMBRE_RESULTADO[valor]}</span>
}

/** Periodos rápidos: devuelve [desde, hasta] en formato AAAA-MM-DD (hasta incluido). */
export function periodo(dias: number | null): [string, string] {
  if (dias === null) return ['', '']
  const hoy = new Date()
  const desde = new Date(hoy.getTime() - (dias - 1) * 86400000)
  const f = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return [f(desde), f(hoy)]
}

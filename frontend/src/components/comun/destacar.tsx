import { Fragment, type ReactNode } from 'react'

/**
 * Muestra un texto del panel resaltando lo que está entre asteriscos:
 * "comenzará *tu próxima historia.*" → "comenzará <em>tu próxima historia.</em>".
 * Siempre como texto (nunca como HTML). Un asterisco sin pareja se muestra tal cual.
 */
export function destacar(texto: string, claseAcento: string): ReactNode {
  const partes = texto.split(/\*([^*]+)\*/g)
  if (partes.length === 1) return texto
  return partes.map((p, i) => (i % 2 === 1 ? <em key={i} className={claseAcento}>{p}</em> : <Fragment key={i}>{p}</Fragment>))
}

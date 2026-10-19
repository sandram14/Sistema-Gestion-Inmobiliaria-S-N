import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { AnalisisConsultas, Conteo, ConteoPropiedad } from '../api/tipos'
import { periodo } from './consultas'
import { erroresDe } from './errores'
import ui from '../ui.module.css'

export type OrigenDato = 'comprobado' | 'inferido'

const EXPLICACION: Record<OrigenDato, string> = {
  comprobado: 'Comprobado: sale de un dato registrado.',
  inferido: 'Inferido: interpretación del texto por palabras clave; puede omitir o confundir casos.',
}

export function Origen({ tipo }: { tipo: OrigenDato }) {
  return (
    <span className={tipo === 'comprobado' ? ui.etiquetaComprobado : ui.etiquetaInferido} title={EXPLICACION[tipo]}>
      {tipo === 'comprobado' ? 'Comprobado' : 'Inferido'}
    </span>
  )
}

export function FichaNumero({ titulo, valor, detalle, origen }: { titulo: string; valor: ReactNode; detalle?: string; origen: OrigenDato }) {
  return (
    <div className={ui.ficha}>
      <div className={ui.fichaCabecera}><span className={ui.fichaTitulo}>{titulo}</span><Origen tipo={origen} /></div>
      <div className={ui.fichaValor}>{valor}</div>
      {detalle && <p className={ui.fichaDetalle}>{detalle}</p>}
    </div>
  )
}

export function Ficha({ titulo, origen, children }: { titulo: string; origen: OrigenDato; children: ReactNode }) {
  return (
    <section className={ui.ficha} aria-label={titulo}>
      <div className={ui.fichaCabecera}><h3 className={ui.fichaTitulo}>{titulo}</h3><Origen tipo={origen} /></div>
      {children}
    </section>
  )
}

/** Un elemento de un gráfico: `texto` es el nombre en texto plano (para la descripción accesible y el aviso al pasar el mouse). */
export interface FilaGrafico { clave: string; etiqueta: ReactNode; texto: string; cantidad: number }

export const deConteo = (filas: Conteo[]): FilaGrafico[] =>
  filas.map((f) => ({ clave: f.nombre, etiqueta: f.nombre, texto: f.nombre, cantidad: f.cantidad }))
export const dePropiedades = (filas: ConteoPropiedad[]): FilaGrafico[] => filas.map((f) => ({
  clave: String(f.propiedad.id),
  etiqueta: <Link to={`/propiedades/${f.propiedad.id}`}>{f.propiedad.titulo} <span className={ui.secundario}>{f.propiedad.codigo}</span></Link>,
  texto: `${f.propiedad.titulo} (${f.propiedad.codigo})`,
  cantidad: f.cantidad,
}))

/** Porcentajes enteros que suman exactamente 100 (método del mayor resto). */
export function porcentajes(cantidades: number[]): number[] {
  const total = cantidades.reduce((s, c) => s + c, 0)
  if (total === 0) return cantidades.map(() => 0)
  const exactos = cantidades.map((c) => (c * 100) / total)
  const enteros = exactos.map(Math.floor)
  let faltan = 100 - enteros.reduce((s, c) => s + c, 0)
  const orden = exactos.map((e, i) => [e - Math.floor(e), i] as const).sort((a, b) => b[0] - a[0])
  for (const [, i] of orden) { if (faltan <= 0) break; enteros[i]++; faltan-- }
  return enteros
}

// Colores de la paleta categórica, en orden fijo (definidos en global.css para tema claro y oscuro)
const COLORES = ['var(--serie-1)', 'var(--serie-2)', 'var(--serie-3)', 'var(--serie-4)', 'var(--serie-5)']
const MAXIMO_PORCIONES = 5
const RADIO = 54
const GROSOR = 20
const CIRCUNFERENCIA = 2 * Math.PI * RADIO
const SEPARACION = 2 // 2 px de fondo entre porciones

/**
 * Gráfico circular (anillo) con porcentajes: las 5 principales y el resto en "Otras". La leyenda, siempre visible,
 * repite nombre, cantidad y porcentaje (la identidad nunca depende solo del color).
 */
export function Torta({ filas, titulo, vacio = 'Sin datos en este periodo.' }: { filas: FilaGrafico[]; titulo: string; vacio?: string }) {
  const [activa, setActiva] = useState<string | null>(null)
  const conDatos = filas.filter((f) => f.cantidad > 0)
  if (conDatos.length === 0) return <p className={ui.secundario}>{vacio}</p>

  const ordenadas = [...conDatos].sort((a, b) => b.cantidad - a.cantidad)
  const principales = ordenadas.slice(0, MAXIMO_PORCIONES)
  const resto = ordenadas.slice(MAXIMO_PORCIONES)
  const porciones = [
    ...principales.map((f, i) => ({ ...f, color: COLORES[i] })),
    ...(resto.length > 0 ? [{
      clave: '__otras', texto: `Otras (${resto.length})`, etiqueta: `Otras (${resto.length})`,
      cantidad: resto.reduce((s, f) => s + f.cantidad, 0), color: 'var(--serie-otras)',
    }] : []),
  ]
  const total = porciones.reduce((s, p) => s + p.cantidad, 0)
  const pct = porcentajes(porciones.map((p) => p.cantidad))
  const descripcion = `${titulo}: ` + porciones.map((p, i) => `${p.texto} ${p.cantidad} (${pct[i]} %)`).join('; ')

  let inicio = 0
  return (
    <div className={ui.torta}>
      <svg className={ui.tortaGrafico} viewBox="0 0 140 140" role="img" aria-label={descripcion}>
        <g transform="rotate(-90 70 70)">
          {porciones.map((p, i) => {
            const largo = (p.cantidad / total) * CIRCUNFERENCIA
            const visible = porciones.length > 1 ? Math.max(largo - SEPARACION, 0.5) : largo
            const desplazamiento = -inicio
            inicio += largo
            return (
              <circle key={p.clave} cx="70" cy="70" r={RADIO} fill="none" stroke={p.color} strokeWidth={GROSOR}
                strokeDasharray={`${visible} ${CIRCUNFERENCIA - visible}`} strokeDashoffset={desplazamiento}
                opacity={activa && activa !== p.clave ? 0.35 : 1}
                onMouseEnter={() => setActiva(p.clave)} onMouseLeave={() => setActiva(null)}
                className={ui.tortaPorcion}>
                <title>{`${p.texto}: ${p.cantidad} (${pct[i]} %)`}</title>
              </circle>
            )
          })}
        </g>
        <text x="70" y="68" textAnchor="middle" className={ui.tortaTotal}>{total}</text>
        <text x="70" y="86" textAnchor="middle" className={ui.tortaTotalTexto}>en total</text>
      </svg>
      <ul className={ui.tortaLeyenda}>
        {porciones.map((p, i) => (
          <li key={p.clave} className={activa === p.clave ? ui.tortaFilaActiva : undefined}
            onMouseEnter={() => setActiva(p.clave)} onMouseLeave={() => setActiva(null)}>
            <span className={ui.tortaMuestra} style={{ background: p.color }} aria-hidden="true" />
            <span className={ui.tortaNombre}>{p.etiqueta}</span>
            <span className={ui.tortaNumero}>{p.cantidad}</span>
            <span className={ui.tortaNumero}>{pct[i]} %</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Medidor circular de una sola proporción (por ejemplo, consultas sin servicio sobre el total). */
export function Medidor({ valor, total, etiqueta, resto }: { valor: number; total: number; etiqueta: string; resto: string }) {
  const proporcion = total > 0 ? valor / total : 0
  const porcentaje = Math.round(proporcion * 100)
  const largo = proporcion * CIRCUNFERENCIA
  return (
    <div className={ui.torta}>
      <svg className={ui.tortaGrafico} viewBox="0 0 140 140" role="img" aria-label={`${etiqueta}: ${valor} de ${total} (${porcentaje} %)`}>
        <circle cx="70" cy="70" r={RADIO} fill="none" stroke="var(--pista-grafico)" strokeWidth={GROSOR} />
        {valor > 0 && (
          <circle cx="70" cy="70" r={RADIO} fill="none" stroke="var(--rojo)" strokeWidth={GROSOR} transform="rotate(-90 70 70)"
            strokeDasharray={`${largo} ${CIRCUNFERENCIA - largo}`}>
            <title>{`${etiqueta}: ${valor} de ${total} (${porcentaje} %)`}</title>
          </circle>
        )}
        <text x="70" y="68" textAnchor="middle" className={ui.tortaTotal}>{porcentaje} %</text>
        <text x="70" y="86" textAnchor="middle" className={ui.tortaTotalTexto}>{etiqueta.toLowerCase()}</text>
      </svg>
      <ul className={ui.tortaLeyenda}>
        <li>
          <span className={ui.tortaMuestra} style={{ background: 'var(--rojo)' }} aria-hidden="true" />
          <span className={ui.tortaNombre}>{etiqueta}</span>
          <span className={ui.tortaNumero}>{valor}</span>
          <span className={ui.tortaNumero}>{porcentaje} %</span>
        </li>
        <li>
          <span className={ui.tortaMuestra} style={{ background: 'var(--pista-grafico)' }} aria-hidden="true" />
          <span className={ui.tortaNombre}>{resto}</span>
          <span className={ui.tortaNumero}>{total - valor}</span>
          <span className={ui.tortaNumero}>{100 - porcentaje} %</span>
        </li>
      </ul>
    </div>
  )
}

/** Selector de periodo y carga del análisis de las consultas. */
export function useAnalisis() {
  const [rango, setRango] = useState<[string, string]>(periodo(30))
  const [datos, setDatos] = useState<AnalisisConsultas | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => {
    const p = new URLSearchParams()
    if (rango[0]) p.set('desde', rango[0])
    if (rango[1]) p.set('hasta', rango[1])
    api.get<AnalisisConsultas>(`/api/admin/consultas/analisis?${p}`).then((r) => { setDatos(r); setError(null) }).catch((e) => setError(erroresDe(e).general))
  }, [rango])
  const filtros = (
    <div className={ui.filtros}>
      <input className={ui.input} type="date" value={rango[0]} onChange={(e) => setRango([e.target.value, rango[1]])} aria-label="Desde" />
      <input className={ui.input} type="date" value={rango[1]} onChange={(e) => setRango([rango[0], e.target.value])} aria-label="Hasta" />
      {[[7, '7 días'], [30, '30 días'], [90, '90 días'], [null, 'Todo']].map(([d, t]) => (
        <button key={String(t)} type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} onClick={() => setRango(periodo(d as number | null))}>{t}</button>
      ))}
    </div>
  )
  return { datos, error, filtros }
}

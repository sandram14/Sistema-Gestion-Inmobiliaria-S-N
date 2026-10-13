import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { AnalisisConsultas, ConsultaResumen, Conteo, ConteoPropiedad } from '../api/tipos'
import { periodo } from './consultas'
import { erroresDe } from './errores'
import { textoFecha } from './formato'
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

export function FichaNumero({ titulo, valor, detalle, origen }: { titulo: string; valor: ReactNode; detalle: string; origen: OrigenDato }) {
  return (
    <div className={ui.ficha}>
      <div className={ui.fichaCabecera}><span className={ui.fichaTitulo}>{titulo}</span><Origen tipo={origen} /></div>
      <div className={ui.fichaValor}>{valor}</div>
      <p className={ui.fichaDetalle}>{detalle}</p>
    </div>
  )
}

export function Ficha({ titulo, nota, origen, children }: { titulo: string; nota: string; origen: OrigenDato; children: ReactNode }) {
  return (
    <section className={ui.ficha}>
      <div className={ui.fichaCabecera}><h3 className={ui.fichaTitulo}>{titulo}</h3><Origen tipo={origen} /></div>
      <p className={ui.fichaDetalle}>{nota}</p>
      {children}
    </section>
  )
}

export function Barras({ filas, vacio = 'Sin datos en este periodo.' }: { filas: { clave: string; etiqueta: ReactNode; cantidad: number }[]; vacio?: string }) {
  if (filas.length === 0) return <p className={ui.secundario}>{vacio}</p>
  const max = Math.max(...filas.map((f) => f.cantidad))
  return (
    <ul className={ui.barras}>
      {filas.slice(0, 5).map((f) => (
        <li key={f.clave}>
          <div className={ui.barraTexto}><span>{f.etiqueta}</span><span className={ui.barraNumero}>{f.cantidad}</span></div>
          <div className={ui.barraFondo}><div className={ui.barraRelleno} style={{ width: `${(f.cantidad * 100) / max}%` }} /></div>
        </li>
      ))}
    </ul>
  )
}

export const deConteo = (filas: Conteo[]) => filas.map((f) => ({ clave: f.nombre, etiqueta: f.nombre, cantidad: f.cantidad }))
export const dePropiedades = (filas: ConteoPropiedad[]) => filas.map((f) => ({
  clave: String(f.propiedad.id),
  etiqueta: <Link to={`/propiedades/${f.propiedad.id}`}>{f.propiedad.titulo} <span className={ui.secundario}>{f.propiedad.codigo}</span></Link>,
  cantidad: f.cantidad,
}))

export function ListaConsultas({ filas }: { filas: ConsultaResumen[] }) {
  if (filas.length === 0) return <p className={ui.secundario}>Ninguna en este periodo.</p>
  return (
    <ul className={ui.listaFicha}>
      {filas.slice(0, 5).map((c) => (
        <li key={c.id}><Link to={`/consultas/${c.id}`}>{c.pregunta}</Link><span className={ui.secundario}>{textoFecha(c.fechaHora)}</span></li>
      ))}
      {filas.length > 5 && <li className={ui.secundario}>Y {filas.length - 5} más en el historial.</li>}
    </ul>
  )
}

/** Selector de periodo y carga del análisis (común a Análisis técnico y Calidad de respuestas). */
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

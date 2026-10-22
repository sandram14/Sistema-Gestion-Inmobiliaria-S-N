import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import PestanasConsultas from '../componentes/PestanasConsultas'
import { api } from '../api/cliente'
import type { PaginaConsultas } from '../api/tipos'
import { Resultado, periodo } from '../componentes/consultas'
import { erroresDe } from '../componentes/errores'
import { textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

/** Historial de consultas del chat IA. El vendedor ve las generales y las de sus propiedades (lo filtra el backend). */
export default function ConsultasHistorial() {
  const [datos, setDatos] = useState<PaginaConsultas | null>(null)
  const [pagina, setPagina] = useState(1)
  const [error, setError] = useState<string | null>(null)
  const [texto, setTexto] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [estado, setEstado] = useState('')
  const [resultado, setResultado] = useState('')

  useEffect(() => {
    const p = new URLSearchParams()
    if (texto.trim()) p.set('texto', texto.trim())
    if (desde) p.set('desde', desde)
    if (hasta) p.set('hasta', hasta)
    if (estado) p.set('estado', estado)
    if (resultado) p.set('resultado', resultado)
    p.set('pagina', String(pagina))
    const t = setTimeout(() => {
      api.get<PaginaConsultas>(`/api/admin/consultas?${p}`).then((r) => { setDatos(r); setError(null) })
        .catch((e) => setError(erroresDe(e).general))
    }, texto ? 300 : 0)
    return () => clearTimeout(t)
  }, [texto, desde, hasta, estado, resultado, pagina])

  // Al cambiar un filtro se vuelve a la primera página
  useEffect(() => setPagina(1), [texto, desde, hasta, estado, resultado])
  const lista = datos?.elementos ?? null
  const paginas = datos ? Math.max(1, Math.ceil(datos.total / datos.tamano)) : 1

  const rapido = (dias: number | null) => { const [d, h] = periodo(dias); setDesde(d); setHasta(h) }

  return (
    <>
      <PestanasConsultas />
      {error && <div className={ui.avisoError} role="alert">{error}</div>}

      <div className={ui.filtros}>
        <input className={ui.input} placeholder="Buscar en preguntas y respuestas" value={texto} maxLength={100} onChange={(e) => setTexto(e.target.value)} aria-label="Buscar" />
        <input className={ui.input} type="date" value={desde} onChange={(e) => setDesde(e.target.value)} aria-label="Desde" />
        <input className={ui.input} type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} aria-label="Hasta" />
        <select className={ui.input} value={resultado} onChange={(e) => setResultado(e.target.value)} aria-label="Resultado">
          <option value="">Todos los resultados</option>
          <option value="RESPONDIDA">Respondida</option>
          <option value="DERIVADA_ASESOR">Derivada a un asesor</option>
          <option value="SIN_SERVICIO">Sin servicio</option>
          <option value="FUERA_DE_AMBITO">Fuera del ámbito</option>
          <option value="NO_REGISTRADO">No registrado</option>
        </select>
        <select className={ui.input} value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Revisión interna">
          <option value="">Revisión: todas</option>
          <option value="PENDIENTE">Sin revisar</option>
          <option value="REVISADA">Revisadas</option>
        </select>
      </div>
      <div className={ui.filtros}>
        {[[7, 'Últimos 7 días'], [30, 'Últimos 30 días'], [90, 'Últimos 90 días'], [null, 'Todo']].map(([d, t]) => (
          <button key={String(t)} type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} onClick={() => rapido(d as number | null)}>{t}</button>
        ))}
      </div>

      {lista === null && !error ? <p className={ui.cargando}>Cargando consultas…</p> : (
        <div className={ui.tablaContenedor}>
          <table className={ui.tabla}>
            <thead><tr><th>Fecha</th><th>Pregunta</th><th>Origen</th><th>Resultado</th><th>Revisión interna</th><th></th></tr></thead>
            <tbody>
              {(lista ?? []).map((c) => (
                <tr key={c.id}>
                  <td className={ui.secundario} style={{ whiteSpace: 'nowrap' }}>{textoFecha(c.fechaHora)}</td>
                  <td>{c.pregunta}</td>
                  <td>{c.propiedad ? <span className={ui.codigo}>{c.propiedad.codigo}</span> : <span className={ui.secundario}>General</span>}</td>
                  <td><Resultado valor={c.resultado} /></td>
                  <td className={ui.secundario}>{c.estadoRevision === 'REVISADA' ? 'Revisada' : 'Sin revisar'}</td>
                  <td><Link to={`/consultas/${c.id}`} className={`${ui.botonSecundario} ${ui.botonChico}`}>Ver</Link></td>
                </tr>
              ))}
              {lista && lista.length === 0 && <tr><td colSpan={6} className={ui.vacio}>No hay consultas para estos filtros.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
      {datos && datos.total > 0 && (
        <div className={ui.paginacion}>
          <span className={ui.pieTabla}>Mostrando {(datos.pagina - 1) * datos.tamano + 1}–{(datos.pagina - 1) * datos.tamano + datos.elementos.length} de {datos.total} consultas</span>
          <div className={ui.acciones}>
            <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={pagina <= 1} onClick={() => setPagina(pagina - 1)} aria-label="Página anterior"><ChevronLeft size={16} aria-hidden="true" /></button>
            <span className={ui.secundario}>Página {pagina} de {paginas}</span>
            <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={pagina >= paginas} onClick={() => setPagina(pagina + 1)} aria-label="Página siguiente"><ChevronRight size={16} aria-hidden="true" /></button>
          </div>
        </div>
      )}
    </>
  )
}

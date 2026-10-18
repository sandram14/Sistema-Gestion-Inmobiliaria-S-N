import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { api } from '../api/cliente'
import type { PaginaClientes } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import PestanasConsultas from '../componentes/PestanasConsultas'
import { EstadoCli, NOMBRE_ORIGEN } from '../componentes/clientes'
import { FichaNumero } from '../componentes/fichas'
import { erroresDe } from '../componentes/errores'
import { textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

/** Clientes potenciales. El vendedor ve solo los asignados a él (lo filtra el backend). */
export default function ClientesPotenciales() {
  const { puede } = useSesion()
  const [datos, setDatos] = useState<PaginaClientes | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [texto, setTexto] = useState('')
  // Desde la ficha de Inicio se llega ya filtrado: /consultas/clientes?estado=PENDIENTE
  const [parametros] = useSearchParams()
  const estadoInicial = parametros.get('estado') ?? ''
  const [estado, setEstado] = useState(['PENDIENTE', 'CONTACTADO', 'DESCARTADO'].includes(estadoInicial) ? estadoInicial : '')
  const [origen, setOrigen] = useState('')
  const [pagina, setPagina] = useState(1)

  useEffect(() => setPagina(1), [texto, estado, origen])
  useEffect(() => {
    const p = new URLSearchParams({ pagina: String(pagina) })
    if (texto.trim()) p.set('texto', texto.trim())
    if (estado) p.set('estado', estado)
    if (origen) p.set('origen', origen)
    const t = setTimeout(() => {
      api.get<PaginaClientes>(`/api/admin/clientes-potenciales?${p}`).then((r) => { setDatos(r); setError(null) }).catch((e) => setError(erroresDe(e).general))
    }, texto ? 300 : 0)
    return () => clearTimeout(t)
  }, [texto, estado, origen, pagina])

  const paginas = datos ? Math.max(1, Math.ceil(datos.total / datos.tamano)) : 1
  const c = datos?.conteos

  return (
    <>
      <PestanasConsultas />
      <div className={ui.encabezadoSeccion}>
        <p className={ui.ayuda}>{puede('CLIENTES_VER_TODOS') ? 'Personas que dejaron sus datos para ser contactadas.' : 'Clientes potenciales asignados a usted.'}</p>
        <Link to="/consultas/clientes/nuevo" className={ui.boton}><Plus size={16} aria-hidden="true" /> Registrar cliente</Link>
      </div>
      {error && <div className={ui.avisoError} role="alert">{error}</div>}
      {c && (
        <div className={ui.gridNumeros}>
          <FichaNumero titulo="Total de clientes potenciales" valor={c.total} detalle="Según los filtros de texto y origen." origen="comprobado" />
          <FichaNumero titulo="Pendientes de contacto" valor={c.pendientes} detalle={c.total ? `${Math.round((c.pendientes * 100) / c.total)} % del total` : ''} origen="comprobado" />
          <FichaNumero titulo="Contactados" valor={c.contactados} detalle={c.total ? `${Math.round((c.contactados * 100) / c.total)} % del total` : ''} origen="comprobado" />
        </div>
      )}
      <div className={ui.filtros}>
        <input className={ui.input} placeholder="Buscar por nombre, teléfono, email o interés" value={texto} maxLength={100} onChange={(e) => setTexto(e.target.value)} aria-label="Buscar" />
        <select className={ui.input} value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Estado de seguimiento">
          <option value="">Todos los estados</option>
          <option value="PENDIENTE">Pendiente de contacto</option>
          <option value="CONTACTADO">Contactado</option>
          <option value="DESCARTADO">Descartado</option>
        </select>
        <select className={ui.input} value={origen} onChange={(e) => setOrigen(e.target.value)} aria-label="Origen">
          <option value="">Todos los orígenes</option>
          {Object.entries(NOMBRE_ORIGEN).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
      </div>
      {datos === null && !error ? <p className={ui.cargando}>Cargando…</p> : (
        <div className={ui.tablaContenedor}>
          <table className={ui.tabla}>
            <thead><tr><th>Nombre</th><th>Contacto</th><th>Interés / Propiedad</th><th>Fecha de registro</th><th>Estado de seguimiento</th><th>Responsable</th><th></th></tr></thead>
            <tbody>
              {(datos?.elementos ?? []).map((cl) => (
                <tr key={cl.id}>
                  <td>{cl.nombre}<div className={ui.secundario}>{NOMBRE_ORIGEN[cl.origen]}</div></td>
                  <td>{cl.contacto}</td>
                  <td>{cl.interes ?? <span className={ui.secundario}>Sin detalle</span>}{cl.propiedad && <div className={ui.secundario}>{cl.propiedad.titulo}</div>}</td>
                  <td className={ui.secundario} style={{ whiteSpace: 'nowrap' }}>{textoFecha(cl.creadoEn)}</td>
                  <td><EstadoCli valor={cl.estado} /></td>
                  <td>{cl.responsable ? cl.responsable.nombre : <span className={ui.secundario}>Sin asignar</span>}</td>
                  <td><Link to={`/consultas/clientes/${cl.id}`} className={`${ui.botonSecundario} ${ui.botonChico}`}>Ver</Link></td>
                </tr>
              ))}
              {datos && datos.elementos.length === 0 && <tr><td colSpan={7} className={ui.vacio}>No hay clientes potenciales para estos filtros.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
      {datos && datos.total > 0 && (
        <div className={ui.paginacion}>
          <span className={ui.pieTabla}>Mostrando {(datos.pagina - 1) * datos.tamano + 1}–{(datos.pagina - 1) * datos.tamano + datos.elementos.length} de {datos.total}</span>
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

import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api, ApiError } from '../api/cliente'
import type { PropiedadResumen } from '../api/tipos'
import { textoFecha, textoOperacion } from '../componentes/formato'
import ui from '../ui.module.css'

export default function ListadoPropiedades() {
  const [propiedades, setPropiedades] = useState<PropiedadResumen[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>((useLocation().state as { mensaje?: string } | null)?.mensaje ?? null)
  const [texto, setTexto] = useState('')
  const [estado, setEstado] = useState('')
  const [tipo, setTipo] = useState('')

  async function cargar() {
    try {
      setPropiedades(await api.get<PropiedadResumen[]>('/api/admin/propiedades'))
      setError(null)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo cargar el listado.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    void cargar()
  }, [])

  const estados = useMemo(() => [...new Set(propiedades.map((p) => p.estado))].sort(), [propiedades])

  const visibles = useMemo(() => {
    const t = texto.trim().toLowerCase()
    return propiedades.filter(
      (p) =>
        (!t || p.codigo.toLowerCase().includes(t) || p.titulo.toLowerCase().includes(t)) &&
        (!estado || p.estado === estado) &&
        (!tipo || p.tipo === tipo)
    )
  }, [propiedades, texto, estado, tipo])

  /** Baja lógica: el backend pasa la propiedad a "Inactivo"; no se borra ningún registro. */
  async function darDeBaja(p: PropiedadResumen) {
    if (!window.confirm(`¿Dar de baja ${p.codigo} "${p.titulo}"?\n\nDejará de mostrarse en la web, pero no se elimina: puede reactivarla cambiando su estado.`)) return
    try {
      await api.delete(`/api/admin/propiedades/${p.id}`)
      setMensaje(`${p.codigo} quedó como Inactivo.`)
      await cargar()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'No se pudo dar de baja la propiedad.')
    }
  }

  return (
    <>
      <div className={ui.tituloPagina}>
        <h1>Propiedades</h1>
        <Link to="/propiedades/nueva" className={ui.boton}>
          + Nueva propiedad
        </Link>
      </div>

      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {error && <div className={ui.avisoError} role="alert">{error}</div>}

      <div className={ui.filtros}>
        <input className={ui.input} placeholder="Buscar por código o título" value={texto}
          onChange={(e) => setTexto(e.target.value)} aria-label="Buscar" />
        <select className={ui.input} value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Estado">
          <option value="">Todos los estados</option>
          {estados.map((e) => <option key={e} value={e}>{e}</option>)}
        </select>
        <select className={ui.input} value={tipo} onChange={(e) => setTipo(e.target.value)} aria-label="Tipo">
          <option value="">Todos los tipos</option>
          <option value="Casa">Casa</option>
          <option value="Terreno">Terreno</option>
        </select>
      </div>

      {cargando ? (
        <p className={ui.cargando}>Cargando propiedades…</p>
      ) : (
        <div className={ui.tablaContenedor}>
          <table className={ui.tabla}>
            <thead>
              <tr>
                <th>Foto</th><th>Código</th><th>Título</th><th>Tipo</th><th>Estado</th>
                <th>Ubicación</th><th>Operaciones</th><th>Actualizado</th><th></th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((p) => (
                <tr key={p.id}>
                  <td>{p.imagenPrincipal ? <img className={ui.miniatura} src={p.imagenPrincipal} alt="" /> : <span className={ui.miniatura} />}</td>
                  <td className={ui.codigo}>{p.codigo}</td>
                  <td>{p.titulo}</td>
                  <td>{p.tipo}</td>
                  <td><span className={`${ui.estado} ${p.estado === 'Inactivo' ? ui.estadoInactivo : ''}`}>{p.estado}</span></td>
                  <td>{p.ciudad}<br /><small>{p.barrio}</small></td>
                  <td>{p.operaciones.map((o) => <div key={o.operacion}>{textoOperacion(o)}</div>)}</td>
                  <td><small>{textoFecha(p.actualizadoEn)}</small></td>
                  <td>
                    <div className={ui.acciones}>
                      <Link to={`/propiedades/${p.id}`} className={`${ui.botonSecundario} ${ui.botonChico}`}>Editar</Link>
                      {p.estado !== 'Inactivo' && (
                        <button className={`${ui.botonPeligro} ${ui.botonChico}`} onClick={() => darDeBaja(p)}>Dar de baja</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {visibles.length === 0 && (
                <tr><td colSpan={9} className={ui.cargando}>No hay propiedades que coincidan con la búsqueda.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
      <p className={ui.ayuda}>{visibles.length} de {propiedades.length} propiedades</p>
    </>
  )
}

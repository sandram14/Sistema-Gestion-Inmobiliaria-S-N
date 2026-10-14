import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowDown, ArrowUp } from 'lucide-react'
import { api } from '../../api/cliente'
import type { Destacada, PropiedadResumen } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { PestanasSitio } from '../../componentes/sitio'
import ui from '../../ui.module.css'

const MAXIMO = 12

/** Propiedades destacadas en la portada: elegir entre las disponibles y ordenar. El inventario se gestiona en Propiedades. */
export default function SitioOfertas() {
  const [lista, setLista] = useState<Destacada[] | null>(null)
  const [propiedades, setPropiedades] = useState<PropiedadResumen[]>([])
  const [elegida, setElegida] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)
  const [cambios, setCambios] = useState(false)

  useEffect(() => {
    api.get<Destacada[]>('/api/admin/sitio/ofertas').then(setLista).catch((e) => setError(erroresDe(e).general))
    api.get<PropiedadResumen[]>('/api/admin/propiedades').then(setPropiedades).catch(() => setPropiedades([]))
  }, [])

  const candidatas = useMemo(() => propiedades.filter((p) => p.estado === 'Disponible' && !lista?.some((d) => d.idPropiedad === p.id)), [propiedades, lista])

  function modificar(nueva: Destacada[]) { setLista(nueva.map((d, i) => ({ ...d, orden: i + 1 }))); setCambios(true); setMensaje(null) }
  function agregar() {
    const p = propiedades.find((x) => x.id === Number(elegida))
    if (!p || !lista) return
    modificar([...lista, { idPropiedad: p.id, codigo: p.codigo, titulo: p.titulo, estado: p.estado, orden: lista.length + 1 }])
    setElegida('')
  }
  function mover(i: number, delta: number) {
    if (!lista) return
    const n = [...lista]; const [x] = n.splice(i, 1); n.splice(i + delta, 0, x); modificar(n)
  }

  async function guardar() {
    setError(null)
    try { setLista(await api.put<Destacada[]>('/api/admin/sitio/ofertas', { ids: (lista ?? []).map((d) => d.idPropiedad) })); setCambios(false); setMensaje('Ofertas publicadas.') }
    catch (e) { setError(erroresDe(e).general) }
  }

  return (
    <>
      <PestanasSitio />
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {error && <div className={ui.avisoError} role="alert">{error}</div>}
      <section className={ui.tarjeta}>
        <h2>Propiedades destacadas en la portada</h2>
        {lista && lista.length === 0 && (
          <div className={ui.aviso}>No hay propiedades seleccionadas: la portada muestra seis propiedades disponibles elegidas al azar.</div>
        )}
        {lista && lista.some((d) => d.estado !== 'Disponible') && (
          <div className={ui.aviso}>Algunas propiedades ya no están disponibles: no se muestran en la portada. Quítelas o deje la selección como está.</div>
        )}
        <div className={ui.filtros}>
          <select className={ui.input} value={elegida} onChange={(e) => setElegida(e.target.value)} aria-label="Propiedad a destacar" disabled={(lista?.length ?? 0) >= MAXIMO}>
            <option value="">Elegir una propiedad disponible…</option>
            {candidatas.map((p) => <option key={p.id} value={p.id}>{p.titulo} ({p.codigo})</option>)}
          </select>
          <button type="button" className={ui.botonSecundario} disabled={!elegida} onClick={agregar}>Agregar</button>
          <span className={ui.secundario}>{lista?.length ?? 0} de {MAXIMO} como máximo</span>
        </div>
        {lista && lista.length > 0 && (
          <div className={ui.tablaContenedor}>
            <table className={ui.tabla}>
              <thead><tr><th className={ui.numero}>Orden</th><th>Propiedad</th><th>Estado</th><th></th></tr></thead>
              <tbody>{lista.map((d, i) => (
                <tr key={d.idPropiedad}>
                  <td className={ui.numero}>{i + 1}</td>
                  <td><Link to={`/propiedades/${d.idPropiedad}`}>{d.titulo}</Link> <span className={ui.secundario}>{d.codigo}</span></td>
                  <td>{d.estado === 'Disponible' ? <span className={ui.estado}>Disponible</span> : <span className={`${ui.estado} ${ui.estadoInactivo}`}>{d.estado}: no se muestra</span>}</td>
                  <td><div className={ui.acciones}>
                    <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={i === 0} onClick={() => mover(i, -1)} aria-label={`Subir ${d.titulo}`}><ArrowUp size={14} aria-hidden="true" /></button>
                    <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={i === lista.length - 1} onClick={() => mover(i, 1)} aria-label={`Bajar ${d.titulo}`}><ArrowDown size={14} aria-hidden="true" /></button>
                    <button type="button" className={`${ui.botonPeligro} ${ui.botonChico}`} onClick={() => modificar(lista.filter((x) => x.idPropiedad !== d.idPropiedad))}>Quitar</button>
                  </div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        <div className={ui.acciones} style={{ marginTop: 12 }}>
          {cambios && <span className={ui.secundario}>Hay cambios sin guardar.</span>}
          <button type="button" className={ui.boton} disabled={!cambios} onClick={() => void guardar()}>Guardar y publicar</button>
        </div>
      </section>
    </>
  )
}

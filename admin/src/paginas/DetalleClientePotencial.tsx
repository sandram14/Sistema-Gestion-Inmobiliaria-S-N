import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { api } from '../api/cliente'
import BotonVolver from '../componentes/BotonVolver'
import type { ClienteDetalle, UsuarioDto } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import { EstadoCli, NOMBRE_ORIGEN } from '../componentes/clientes'
import { erroresDe } from '../componentes/errores'
import { textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

export default function DetalleClientePotencial() {
  const { id } = useParams()
  const { puede } = useSesion()
  const [c, setC] = useState<ClienteDetalle | null>(null)
  const [usuarios, setUsuarios] = useState<UsuarioDto[]>([])
  const [error, setError] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>((useLocation().state as { mensaje?: string } | null)?.mensaje ?? null)

  useEffect(() => {
    api.get<ClienteDetalle>(`/api/admin/clientes-potenciales/${id}`).then(setC).catch((e) => setError(erroresDe(e).general))
    if (puede('CLIENTES_ASIGNAR')) api.get<UsuarioDto[]>('/api/admin/usuarios').then(setUsuarios).catch(() => setUsuarios([]))
  }, [id, puede])

  async function cambiar(ruta: 'estado' | 'responsable', cuerpo: object, texto: string) {
    setError(null); setMensaje(null)
    try { setC(await api.patch<ClienteDetalle>(`/api/admin/clientes-potenciales/${id}/${ruta}`, cuerpo)); setMensaje(texto) } catch (e) { setError(erroresDe(e).general) }
  }

  const volver = <BotonVolver alternativa="/consultas/clientes" />
  if (!c) return <><div className={ui.tituloPagina}><div className={ui.tituloConVolver}>{volver}<h1>Cliente potencial</h1></div></div>{error ? <div className={ui.avisoError}>{error}</div> : <p className={ui.cargando}>Cargando…</p>}</>

  return (
    <>
      <div className={ui.tituloPagina}><div className={ui.tituloConVolver}>{volver}<h1>{c.nombre}</h1></div></div>
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {error && <div className={ui.avisoError} role="alert">{error}</div>}

      <section className={ui.tarjeta}>
        <h2>Datos de contacto</h2>
        <dl className={ui.datos}>
          <dt>Teléfono</dt><dd>{c.telefono ? <a href={`tel:${c.telefono.replace(/[^+0-9]/g, '')}`}>{c.telefono}</a> : <span className={ui.secundario}>No indicado</span>}</dd>
          <dt>Email</dt><dd>{c.email ? <a href={`mailto:${c.email}`}>{c.email}</a> : <span className={ui.secundario}>No indicado</span>}</dd>
          <dt>Detalle de su interés</dt><dd style={{ whiteSpace: 'pre-line' }}>{c.interes ?? <span className={ui.secundario}>Sin detalle</span>}</dd>
          <dt>Propiedad de interés</dt><dd>{c.propiedad ? <Link to={`/propiedades/${c.propiedad.id}`}>{c.propiedad.titulo} ({c.propiedad.codigo})</Link> : <span className={ui.secundario}>Ninguna</span>}</dd>
          <dt>Consulta relacionada</dt><dd>{c.idConsulta ? <Link to={`/consultas/${c.idConsulta}`}>Ver la consulta del chat</Link> : <span className={ui.secundario}>Ninguna</span>}</dd>
        </dl>
      </section>

      <section className={ui.tarjeta}>
        <h2>Seguimiento</h2>
        <dl className={ui.datos}>
          <dt>Estado de seguimiento</dt>
          <dd><div className={ui.filaControl}><EstadoCli valor={c.estado} />
            <select className={ui.input} aria-label="Cambiar estado" value={c.estado} onChange={(e) => void cambiar('estado', { estado: e.target.value }, 'Estado actualizado.')}>
              <option value="PENDIENTE">Pendiente de contacto</option><option value="CONTACTADO">Contactado</option><option value="DESCARTADO">Descartado</option>
            </select></div></dd>
          <dt>Responsable</dt>
          <dd>{puede('CLIENTES_ASIGNAR') ? (
            <select className={ui.input} aria-label="Responsable" value={c.responsable?.id ?? ''}
              onChange={(e) => void cambiar('responsable', { idResponsable: e.target.value ? Number(e.target.value) : null }, 'Responsable actualizado.')}>
              <option value="">Sin asignar</option>
              {usuarios.filter((u) => u.activo || u.id === c.responsable?.id).map((u) => <option key={u.id} value={u.id}>{u.nombre} {u.apellido}</option>)}
            </select>
          ) : (c.responsable?.nombre ?? <span className={ui.secundario}>Sin asignar</span>)}</dd>
        </dl>
      </section>

      <section className={ui.tarjeta}>
        <h2>Registro</h2>
        <dl className={ui.datos}>
          <dt>Origen</dt><dd>{NOMBRE_ORIGEN[c.origen]}</dd>
          <dt>Fecha de registro</dt><dd>{textoFecha(c.creadoEn)}</dd>
          <dt>Registrado por</dt><dd>{c.registradoPor ? c.registradoPor.nombre : c.origen === 'CONTACTO' ? 'El propio cliente, desde el formulario de contacto' : 'El propio cliente, desde el chat'}</dd>
          <dt>Aceptación del tratamiento</dt>
          <dd>{c.consentimientoEn ? `Aceptó el ${textoFecha(c.consentimientoEn)} (texto versión ${c.consentimientoVersion})` : <span className={ui.secundario}>No aplica: registrado por el equipo a partir de un contacto directo</span>}</dd>
        </dl>
      </section>
    </>
  )
}

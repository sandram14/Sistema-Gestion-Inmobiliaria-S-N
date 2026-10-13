import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'
import { api } from '../api/cliente'
import type { ConsultaDetalle, PropiedadRef } from '../api/tipos'
import { NO_REGISTRADO, NOTA_REVISION, Resultado } from '../componentes/consultas'
import { erroresDe } from '../componentes/errores'
import { textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

const enlace = (p: PropiedadRef) => <Link key={p.id} to={`/propiedades/${p.id}`}>{p.codigo} · {p.titulo}</Link>

export default function DetalleConsulta() {
  const { id } = useParams()
  const [c, setC] = useState<ConsultaDetalle | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setC(null)
    api.get<ConsultaDetalle>(`/api/admin/consultas/${id}`).then(setC).catch((e) => setError(erroresDe(e).general))
  }, [id])

  async function revisar(estado: 'REVISADA' | 'PENDIENTE') {
    try { setC(await api.patch<ConsultaDetalle>(`/api/admin/consultas/${id}/revision`, { estado })) } catch (e) { setError(erroresDe(e).general) }
  }

  const volver = <Link to="/consultas" className={ui.botonSecundario}><ChevronLeft size={16} aria-hidden="true" /> Volver al historial</Link>
  if (error && !c) return <><div className={ui.tituloPagina}><h1>Consulta</h1>{volver}</div><div className={ui.avisoError}>{error}</div></>
  if (!c) return <p className={ui.cargando}>Cargando…</p>

  return (
    <>
      <div className={ui.tituloPagina}><h1>Consulta del {textoFecha(c.fechaHora)}</h1>{volver}</div>
      {error && <div className={ui.avisoError} role="alert">{error}</div>}

      <section className={ui.tarjeta}>
        <h2>Pregunta del cliente</h2>
        <p style={{ whiteSpace: 'pre-line', margin: 0 }}>{c.pregunta}</p>
      </section>
      <section className={ui.tarjeta}>
        <h2>Respuesta del asistente</h2>
        <p style={{ whiteSpace: 'pre-line', margin: 0 }}>{c.respuesta ?? <span className={ui.secundario}>Sin respuesta registrada</span>}</p>
      </section>

      <section className={ui.tarjeta}>
        <h2>Datos de la consulta</h2>
        <dl className={ui.datos}>
          <dt>Origen</dt><dd>{c.propiedad ? <>Desde la propiedad {enlace(c.propiedad)}</> : 'General (no se inició desde una propiedad)'}</dd>
          <dt>Resultado</dt><dd>{c.registroCompleto ? <Resultado valor={c.resultado} /> : <span className={ui.secundario}>{NO_REGISTRADO}</span>}</dd>
          <dt>Propiedades recomendadas</dt>
          <dd>{!c.registroCompleto ? <span className={ui.secundario}>{NO_REGISTRADO}</span>
            : c.recomendadas.length === 0 ? <span className={ui.secundario}>Ninguna</span>
            : <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>{c.recomendadas.map(enlace)}</div>}</dd>
        </dl>
      </section>

      <section className={ui.tarjeta}>
        <h2>Revisión interna</h2>
        <p className={ui.ayuda} style={{ marginTop: 0 }}>{NOTA_REVISION}</p>
        <dl className={ui.datos}>
          <dt>Estado</dt><dd>{c.estadoRevision === 'REVISADA' ? `Revisada por ${c.revisadaPor ?? 'un usuario eliminado'} el ${textoFecha(c.revisadaEn)}` : 'Sin revisar'}</dd>
        </dl>
        <div className={ui.acciones}>
          {c.estadoRevision === 'REVISADA'
            ? <button type="button" className={ui.botonSecundario} onClick={() => void revisar('PENDIENTE')}>Marcar como sin revisar</button>
            : <button type="button" className={ui.boton} onClick={() => void revisar('REVISADA')}>Marcar como revisada</button>}
        </div>
      </section>

      {c.conversacion.length > 1 && (
        <section className={ui.tarjeta}>
          <h2>Otras preguntas de esta conversación</h2>
          <table className={ui.tabla}><tbody>
            {c.conversacion.map((o) => (
              <tr key={o.id}>
                <td className={ui.secundario} style={{ whiteSpace: 'nowrap' }}>{textoFecha(o.fechaHora)}</td>
                <td>{o.actual ? <strong>{o.pregunta} (esta consulta)</strong> : <Link to={`/consultas/${o.id}`}>{o.pregunta}</Link>}</td>
              </tr>
            ))}
          </tbody></table>
        </section>
      )}
    </>
  )
}

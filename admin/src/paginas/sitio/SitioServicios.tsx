import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import type { ContenidoServicios } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { ListaItems, PestanasSitio } from '../../componentes/sitio'
import ui from '../../ui.module.css'

export default function SitioServicios() {
  const [c, setC] = useState<ContenidoServicios | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEffect(() => { api.get<ContenidoServicios>('/api/admin/sitio/secciones/servicios').then(setC).catch((e) => setGeneral(erroresDe(e).general)) }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null); setMensaje(null)
    try { setC(await api.put<ContenidoServicios>('/api/admin/sitio/secciones/servicios', c)); setMensaje('Servicios publicados.') }
    catch (err) { const r = erroresDe(err); setCampos(r.campos); setGeneral(r.general) }
  }

  const texto = (k: 'titulo' | 'subtitulo' | 'bloqueTitulo' | 'bloqueTexto', etiqueta: string, max: number, area = false) => (
    <label className={`${ui.campo} ${area ? ui.campoAncho : ''}`}><span className={ui.etiqueta}>{etiqueta}</span>
      {area ? <textarea className={ui.input} rows={3} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />
        : <input className={ui.input} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />}
      {campos[k] && <span className={ui.errorCampo}>{campos[k]}</span>}</label>
  )

  return (
    <>
      <PestanasSitio />
      <p className={ui.ayuda}>Para destacar palabras de un título en verde e itálica, escribilas entre asteriscos. Ejemplo: <code>comenzará *tu próxima historia.*</code></p>
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      {!c ? (!general && <p className={ui.cargando}>Cargando…</p>) : (
        <form onSubmit={guardar} noValidate>
          <section className={ui.tarjeta}>
            <h2>Encabezado</h2>
            <div className={ui.grilla}>{texto('titulo', 'Título de la página', 80)}{texto('subtitulo', 'Subtítulo', 200)}</div>
          </section>
          <section className={ui.tarjeta}>
            <h2>Servicios</h2>
            <ListaItems items={c.servicios} onCambio={(l) => setC({ ...c, servicios: l })} min={1} max={8} prefijo="servicios" errores={campos} nombre="Servicio" />
          </section>
          <section className={ui.tarjeta}>
            <h2>Bloque inferior</h2>
            <div className={ui.grilla}>{texto('bloqueTitulo', 'Título del bloque', 120)}{texto('bloqueTexto', 'Texto del bloque', 600, true)}</div>
          </section>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar y publicar</button></div>
        </form>
      )}
    </>
  )
}

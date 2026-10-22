import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import type { ContenidoInicio } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { ImagenSitio, PestanasSitio } from '../../componentes/sitio'
import OfertasDestacadas from '../../componentes/OfertasDestacadas'
import ui from '../../ui.module.css'

export default function SitioInicio() {
  const [c, setC] = useState<ContenidoInicio | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEffect(() => { api.get<ContenidoInicio>('/api/admin/sitio/secciones/inicio').then(setC).catch((e) => setGeneral(erroresDe(e).general)) }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null); setMensaje(null)
    if (!c) return
    // Solo los campos editables de la portada (se retiraron el bloque "Sobre nosotros" y el texto pequeño sobre el título)
    const cuerpo = { imagenPortada: c.imagenPortada, titulo: c.titulo, subtitulo: c.subtitulo }
    try { setC(await api.put<ContenidoInicio>('/api/admin/sitio/secciones/inicio', cuerpo)); setMensaje('Portada publicada.') }
    catch (err) { const r = erroresDe(err); setCampos(r.campos); setGeneral(r.general) }
  }

  const cambiar = (k: keyof ContenidoInicio, v: string | null) => c && setC({ ...c, [k]: v })
  const err = (k: string) => campos[k] && <span className={ui.errorCampo}>{campos[k]}</span>
  const texto = (k: keyof ContenidoInicio, etiqueta: string, max: number, area = false) => (
    <label className={`${ui.campo} ${area ? ui.campoAncho : ''}`}><span className={ui.etiqueta}>{etiqueta}</span>
      {area ? <textarea className={ui.input} rows={3} maxLength={max} value={(c?.[k] as string) ?? ''} onChange={(e) => cambiar(k, e.target.value)} />
        : <input className={ui.input} maxLength={max} value={(c?.[k] as string) ?? ''} onChange={(e) => cambiar(k, e.target.value)} />}
      {err(k)}</label>
  )

  return (
    <>
      <PestanasSitio />
      <p className={ui.ayuda}>Para destacar palabras de un título en verde, escribilas entre asteriscos. Ejemplo: <code>comenzará *tu próxima historia.*</code></p>
      {!c ? (!general && <p className={ui.cargando}>Cargando…</p>) : (
        <form onSubmit={guardar} noValidate>
          <section className={ui.tarjeta}>
            <h2>Portada</h2>
            {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
            {general && <div className={ui.avisoError} role="alert">{general}</div>}
            <div className={ui.grilla}>
              <ImagenSitio etiqueta="Imagen de fondo" valor={c.imagenPortada} onCambio={(u) => cambiar('imagenPortada', u)} />
              {texto('titulo', 'Título de la portada', 120)}
              {texto('subtitulo', 'Texto introductorio', 300, true)}
            </div>
            {err('imagenPortada')}
            <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar y publicar la portada</button></div>
          </section>
        </form>
      )}
      <OfertasDestacadas />
    </>
  )
}

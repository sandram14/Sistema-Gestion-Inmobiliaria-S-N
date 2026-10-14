import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import type { ContenidoInicio } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { ImagenSitio, PestanasSitio } from '../../componentes/sitio'
import ui from '../../ui.module.css'

export default function SitioInicio() {
  const [c, setC] = useState<ContenidoInicio | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEffect(() => { api.get<ContenidoInicio>('/api/admin/sitio/secciones/inicio').then(setC).catch((e) => setGeneral(erroresDe(e).general)) }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null); setMensaje(null)
    try { setC(await api.put<ContenidoInicio>('/api/admin/sitio/secciones/inicio', c)); setMensaje('Cambios publicados.') }
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
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      {!c ? (!general && <p className={ui.cargando}>Cargando…</p>) : (
        <form onSubmit={guardar} noValidate>
          <section className={ui.tarjeta}>
            <h2>Portada</h2>
            <div className={ui.grilla}>
              <ImagenSitio etiqueta="Imagen de fondo" valor={c.imagenPortada} onCambio={(u) => cambiar('imagenPortada', u)} />
              <div className={ui.campo}>
                {texto('etiqueta', 'Texto pequeño sobre el título (opcional)', 80)}
                {texto('titulo', 'Título de la portada', 120)}
              </div>
              {texto('subtitulo', 'Texto introductorio', 300, true)}
            </div>
            {err('imagenPortada')}
          </section>
          <section className={ui.tarjeta}>
            <h2>Bloque "Sobre nosotros" de la portada</h2>
            <div className={ui.grilla}>
              <ImagenSitio etiqueta="Imagen" valor={c.nosotrosImagen} onCambio={(u) => cambiar('nosotrosImagen', u)} />
              <div className={ui.campo}>{texto('nosotrosTitulo', 'Título del bloque', 120)}</div>
              {texto('nosotrosTexto', 'Texto del bloque', 1200, true)}
            </div>
          </section>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar y publicar</button></div>
        </form>
      )}
    </>
  )
}

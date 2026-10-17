import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import type { ContenidoNosotros } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { ImagenSitio, ListaItems, PestanasSitio } from '../../componentes/sitio'
import ui from '../../ui.module.css'

export default function SitioNosotros() {
  const [c, setC] = useState<ContenidoNosotros | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEffect(() => { api.get<ContenidoNosotros>('/api/admin/sitio/secciones/nosotros').then(setC).catch((e) => setGeneral(erroresDe(e).general)) }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null); setMensaje(null)
    try { setC(await api.put<ContenidoNosotros>('/api/admin/sitio/secciones/nosotros', c)); setMensaje('Página Nosotros publicada.') }
    catch (err) { const r = erroresDe(err); setCampos(r.campos); setGeneral(r.general) }
  }

  const texto = (k: 'introduccion' | 'historiaTitulo' | 'historiaTexto', etiqueta: string, max: number, area = false) => (
    <label className={`${ui.campo} ${area ? ui.campoAncho : ''}`}><span className={ui.etiqueta}>{etiqueta}</span>
      {area ? <textarea className={ui.input} rows={4} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />
        : <input className={ui.input} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />}
      {campos[k] && <span className={ui.errorCampo}>{campos[k]}</span>}</label>
  )
  const cifra = (i: number, k: 'valor' | 'etiqueta', v: string) => c && setC({ ...c, cifras: c.cifras.map((x, j) => (j === i ? { ...x, [k]: v } : x)) })

  return (
    <>
      <PestanasSitio />
      <p className={ui.ayuda}>Para destacar palabras de un título en verde e itálica, escribilas entre asteriscos. Ejemplo: <code>comenzará *tu próxima historia.*</code></p>
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      {!c ? (!general && <p className={ui.cargando}>Cargando…</p>) : (
        <form onSubmit={guardar} noValidate>
          <section className={ui.tarjeta}>
            <h2>Nuestra historia</h2>
            <div className={ui.grilla}>
              {texto('introduccion', 'Introducción (encabezado de la página)', 600, true)}
              <ImagenSitio etiqueta="Imagen principal" valor={c.imagen} onCambio={(u) => setC({ ...c, imagen: u })} />
              {texto('historiaTitulo', 'Título de la historia', 120)}
              {texto('historiaTexto', 'Texto de la historia', 1500, true)}
            </div>
            {campos.imagen && <span className={ui.errorCampo}>{campos.imagen}</span>}
          </section>
          <section className={ui.tarjeta}>
            <h2>Cifras</h2>
            <p className={ui.ayuda}>Datos destacados (por ejemplo "+150 · Propiedades"). Verifique que sean reales. Hasta 4.</p>
            {c.cifras.map((x, i) => (
              <div key={i} className={ui.filaCifra}>
                <label className={ui.campo}><span className={ui.etiqueta}>Cifra {i + 1}: valor</span>
                  <input className={ui.input} maxLength={12} value={x.valor} onChange={(e) => cifra(i, 'valor', e.target.value)} />
                  {campos[`cifras[${i}].valor`] && <span className={ui.errorCampo}>{campos[`cifras[${i}].valor`]}</span>}</label>
                <label className={ui.campo}><span className={ui.etiqueta}>Cifra {i + 1}: descripción</span>
                  <input className={ui.input} maxLength={40} value={x.etiqueta} onChange={(e) => cifra(i, 'etiqueta', e.target.value)} />
                  {campos[`cifras[${i}].etiqueta`] && <span className={ui.errorCampo}>{campos[`cifras[${i}].etiqueta`]}</span>}</label>
                <button type="button" className={`${ui.botonPeligro} ${ui.botonChico}`} onClick={() => setC({ ...c, cifras: c.cifras.filter((_, j) => j !== i) })}>Quitar</button>
              </div>
            ))}
            <button type="button" className={ui.botonSecundario} disabled={c.cifras.length >= 4} onClick={() => setC({ ...c, cifras: [...c.cifras, { valor: '', etiqueta: '' }] })}>Agregar cifra</button>
          </section>
          <section className={ui.tarjeta}>
            <h2>Nuestros valores</h2>
            <ListaItems items={c.valores} onCambio={(l) => setC({ ...c, valores: l })} min={1} max={6} prefijo="valores" errores={campos} nombre="Valor" />
          </section>
          <section className={ui.tarjeta}>
            <h2>Por qué elegirnos</h2>
            <ListaItems items={c.razones} onCambio={(l) => setC({ ...c, razones: l })} min={1} max={6} prefijo="razones" errores={campos} nombre="Razón" />
          </section>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar y publicar</button></div>
        </form>
      )}
    </>
  )
}

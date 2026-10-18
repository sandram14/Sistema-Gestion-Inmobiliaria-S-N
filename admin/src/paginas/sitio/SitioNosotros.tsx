import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import type { ContenidoNosotros, ContenidoServicios } from '../../api/tipos'
import { erroresDe } from '../../componentes/errores'
import { ImagenSitio, ListaItems, PestanasSitio } from '../../componentes/sitio'
import ui from '../../ui.module.css'

/**
 * Página Nosotros del sitio: historia, cifras, valores y servicios (los servicios eran una pestaña aparte).
 * Se guardan las dos secciones (NOSOTROS y SERVICIOS) con un solo botón. "Por qué elegirnos" y el bloque
 * inferior de Servicios se retiraron del sitio: se guardan vacíos.
 */
export default function SitioNosotros() {
  const [c, setC] = useState<ContenidoNosotros | null>(null)
  const [s, setS] = useState<ContenidoServicios | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([
      api.get<ContenidoNosotros>('/api/admin/sitio/secciones/nosotros'),
      api.get<ContenidoServicios>('/api/admin/sitio/secciones/servicios'),
    ]).then(([n, sv]) => { setC(n); setS(sv) }).catch((e) => setGeneral(erroresDe(e).general))
  }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault(); setCampos({}); setGeneral(null); setMensaje(null)
    if (!c || !s) return
    const errores: Record<string, string> = {}
    const fallas: string[] = []
    let historiaOk = false
    let serviciosOk = false
    try { setC(await api.put<ContenidoNosotros>('/api/admin/sitio/secciones/nosotros', { ...c, razones: [] })); historiaOk = true }
    catch (err) { const r = erroresDe(err); Object.assign(errores, r.campos); fallas.push(`Historia y valores: ${r.general}`) }
    try { setS(await api.put<ContenidoServicios>('/api/admin/sitio/secciones/servicios', { ...s, bloqueTitulo: '', bloqueTexto: '' })); serviciosOk = true }
    catch (err) { const r = erroresDe(err); Object.assign(errores, r.campos); fallas.push(`Servicios: ${r.general}`) }
    setCampos(errores)
    if (historiaOk && serviciosOk) setMensaje('Página Nosotros publicada (historia, valores y servicios).')
    else {
      // Se dice con precisión qué se publicó y qué no
      const publicado = historiaOk ? ' Sí se publicaron la historia y los valores.' : serviciosOk ? ' Sí se publicaron los servicios.' : ''
      setGeneral(`${fallas.join(' ')}${publicado}`)
    }
  }

  const texto = (k: 'introduccion' | 'historiaTitulo' | 'historiaTexto', etiqueta: string, max: number, area = false) => (
    <label className={`${ui.campo} ${area ? ui.campoAncho : ''}`}><span className={ui.etiqueta}>{etiqueta}</span>
      {area ? <textarea className={ui.input} rows={4} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />
        : <input className={ui.input} maxLength={max} value={c?.[k] ?? ''} onChange={(e) => c && setC({ ...c, [k]: e.target.value })} />}
      {campos[k] && <span className={ui.errorCampo}>{campos[k]}</span>}</label>
  )
  const textoServicios = (k: 'titulo' | 'subtitulo', etiqueta: string, max: number) => (
    <label className={ui.campo}><span className={ui.etiqueta}>{etiqueta}</span>
      <input className={ui.input} maxLength={max} value={s?.[k] ?? ''} onChange={(e) => s && setS({ ...s, [k]: e.target.value })} />
      {campos[k] && <span className={ui.errorCampo}>{campos[k]}</span>}</label>
  )
  const cifra = (i: number, k: 'valor' | 'etiqueta', v: string) => c && setC({ ...c, cifras: c.cifras.map((x, j) => (j === i ? { ...x, [k]: v } : x)) })

  return (
    <>
      <PestanasSitio />
      <p className={ui.ayuda}>Para destacar palabras de un título en verde, escribilas entre asteriscos. Ejemplo: <code>comenzará *tu próxima historia.*</code></p>
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      {!c || !s ? (!general && <p className={ui.cargando}>Cargando…</p>) : (
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
            <h2>Servicios</h2>
            <p className={ui.ayuda}>Se muestran en la página Nosotros, debajo de los valores.</p>
            <div className={ui.grilla}>
              {textoServicios('titulo', 'Título de los servicios', 80)}
              {textoServicios('subtitulo', 'Subtítulo', 200)}
            </div>
            <div style={{ marginTop: 16 }}>
              <ListaItems items={s.servicios} onCambio={(l) => setS({ ...s, servicios: l })} min={1} max={8} prefijo="servicios" errores={campos} nombre="Servicio" />
            </div>
          </section>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar y publicar</button></div>
        </form>
      )}
    </>
  )
}

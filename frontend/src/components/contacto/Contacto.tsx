import { useState, type FormEvent } from 'react'
import { Clock, Mail, MapPin, MessageCircle, Phone } from 'lucide-react'
import { ApiError } from '../../services/api'
import { useContenidoSitio } from '../../services/contenidoSitio'
import { enviarClientePotencial, TEXTO_ACEPTACION } from '../../services/clientesPotenciales'
import styles from './Contacto.module.css'

const ASUNTOS = ['Comprar', 'Alquilar', 'Vender o alquilar mi propiedad', 'Otra consulta']
const VACIO = { nombre: '', telefono: '', email: '', asunto: '', mensaje: '', acepta: false }

/** Página Contacto institucional: canales configurados en el panel y formulario de contacto. */
export default function Contacto() {
  const { contenido } = useContenidoSitio()
  const c = contenido.contacto
  const provisional = contenido.contactoProvisional
  const [f, setF] = useState(VACIO)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [enviado, setEnviado] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  function validar(): Record<string, string> {
    const e: Record<string, string> = {}
    if (f.nombre.trim().length < 2) e.nombre = 'Ingresá tu nombre'
    if (!f.telefono.trim() && !f.email.trim()) e.contacto = 'Indicá un teléfono o un email para que podamos contactarte'
    if (!f.acepta) e.aceptaTratamiento = 'Para enviar el mensaje tenés que aceptar el uso de tus datos'
    return e
  }

  async function enviar(ev: FormEvent) {
    ev.preventDefault()
    setGeneral(null)
    const e = validar()
    setErrores(e)
    if (Object.keys(e).length > 0) return
    setEnviando(true)
    try {
      const mensaje = (f.asunto ? `Asunto: ${f.asunto}\n` : '') + f.mensaje.trim()
      const r = await enviarClientePotencial({ nombre: f.nombre, telefono: f.telefono, email: f.email, mensaje, aceptaTratamiento: f.acepta }, 'CONTACTO')
      setEnviado(r.mensaje)
      setF(VACIO)
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.campos).length > 0) {
        const { interes, ...resto } = err.campos
        setErrores(interes ? { ...resto, mensaje: interes } : resto)
      } else {
        setGeneral(err instanceof ApiError ? err.message : 'No se pudo enviar. Intentá más tarde o escribinos por WhatsApp.')
      }
    } finally {
      setEnviando(false)
    }
  }

  const err = (k: string) => errores[k] && <span className={styles.error}>{errores[k]}</span>
  const telefonoEnlace = c.telefono.replace(/[^+0-9]/g, '')
  const ubicacion = [c.direccion, c.ciudad].filter(Boolean).join(', ')

  return (
    <section className={styles.page}>
      <header className={styles.encabezado}>
        <h1 className={styles.title}>Tu consulta nos interesa, ponte en contacto con nosotros</h1>
      </header>

      <div className={styles.grid}>
        {/* Canales institucionales (configurados en el panel) */}
        <div className={styles.canales}>
          {ubicacion && (
            <div className={styles.canal}>
              <span className={styles.canalIcono}><MapPin size={20} aria-hidden="true" /></span>
              <div><h2 className={styles.canalTitulo}>Dirección</h2><p className={styles.canalTexto}>{ubicacion}</p></div>
            </div>
          )}
          {provisional ? (
            <p className={styles.actualizacion}>Datos de contacto en actualización. Mientras tanto, podés escribirnos con el formulario.</p>
          ) : (
            <>
              {c.email && (
                <div className={styles.canal}>
                  <span className={styles.canalIcono}><Mail size={20} aria-hidden="true" /></span>
                  <div><h2 className={styles.canalTitulo}>Escribinos</h2><a className={styles.canalEnlace} href={`mailto:${c.email}`}>{c.email}</a></div>
                </div>
              )}
              {c.telefono && (
                <div className={styles.canal}>
                  <span className={styles.canalIcono}><Phone size={20} aria-hidden="true" /></span>
                  <div><h2 className={styles.canalTitulo}>Llamanos</h2><a className={styles.canalEnlace} href={`tel:${telefonoEnlace}`}>{c.telefono}</a></div>
                </div>
              )}
              {c.whatsapp && (
                <div className={styles.canal}>
                  <span className={styles.canalIcono}><MessageCircle size={20} aria-hidden="true" /></span>
                  <div><h2 className={styles.canalTitulo}>WhatsApp</h2>
                    <a className={styles.canalEnlace} href={`https://wa.me/${c.whatsapp}`} target="_blank" rel="noopener noreferrer">+{c.whatsapp}</a></div>
                </div>
              )}
            </>
          )}
          {c.horario && (
            <div className={styles.canal}>
              <span className={styles.canalIcono}><Clock size={20} aria-hidden="true" /></span>
              <div><h2 className={styles.canalTitulo}>Horario de atención</h2><p className={styles.canalTexto}>{c.horario}</p></div>
            </div>
          )}
        </div>

        {/* Formulario: llega al panel como cliente potencial ("Formulario de contacto") */}
        <form className={styles.formulario} onSubmit={enviar} noValidate>
          {enviado && <div className={styles.exito} role="status">{enviado}</div>}
          {general && <div className={styles.errorGeneral} role="alert">{general}</div>}
          <div className={styles.filas}>
            <label className={styles.campo}><span className={styles.etiqueta}>Nombre y apellido</span>
              <input className={styles.input} maxLength={120} autoComplete="name" value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} />{err('nombre')}</label>
            <label className={styles.campo}><span className={styles.etiqueta}>Asunto (opcional)</span>
              <select className={styles.input} value={f.asunto} onChange={(e) => setF({ ...f, asunto: e.target.value })}>
                <option value="">Seleccionar asunto</option>
                {ASUNTOS.map((a) => <option key={a} value={a}>{a}</option>)}
              </select></label>
            <label className={styles.campo}><span className={styles.etiqueta}>Teléfono o celular</span>
              <input className={styles.input} type="tel" maxLength={30} autoComplete="tel" value={f.telefono} onChange={(e) => setF({ ...f, telefono: e.target.value })} />{err('telefono')}</label>
            <label className={styles.campo}><span className={styles.etiqueta}>Email</span>
              <input className={styles.input} type="email" maxLength={120} autoComplete="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />{err('email')}</label>
          </div>
          {err('contacto')}
          <label className={styles.campo}><span className={styles.etiqueta}>Mensaje</span>
            <textarea className={styles.input} rows={5} maxLength={900} value={f.mensaje} onChange={(e) => setF({ ...f, mensaje: e.target.value })} />{err('mensaje')}</label>
          <label className={styles.aceptacion}>
            <input type="checkbox" checked={f.acepta} onChange={(e) => setF({ ...f, acepta: e.target.checked })} />
            <span>{TEXTO_ACEPTACION}</span>
          </label>
          {err('aceptaTratamiento')}
          <button type="submit" className={styles.enviar} disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar mensaje'}</button>
        </form>
      </div>
    </section>
  )
}

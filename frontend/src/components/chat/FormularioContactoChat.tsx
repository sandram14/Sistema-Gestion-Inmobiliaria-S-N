import { useState, type FormEvent } from 'react'
import { ApiError } from '../../services/api'
import { enviarClientePotencial, TEXTO_ACEPTACION } from '../../services/clientesPotenciales'
import styles from './AssistantWidget.module.css'

/**
 * Formulario breve dentro del chat: el visitante deja sus datos para que un asesor lo contacte.
 * Llega al panel como cliente potencial con origen "Chat del sitio", vinculado a su consulta.
 */
export default function FormularioContactoChat({ mensajeInicial, codigoPropiedad, nombreInicial = '', telefonoInicial = '' }: {
  mensajeInicial: string
  codigoPropiedad?: string | null
  nombreInicial?: string
  telefonoInicial?: string
}) {
  const [f, setF] = useState({ nombre: nombreInicial, telefono: telefonoInicial, email: '', mensaje: mensajeInicial, acepta: false })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [enviado, setEnviado] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setGeneral(null)
    const v: Record<string, string> = {}
    if (f.nombre.trim().length < 2) v.nombre = 'Ingresá tu nombre'
    if (!f.telefono.trim() && !f.email.trim()) v.contacto = 'Indicá un teléfono o un email'
    if (!f.acepta) v.aceptaTratamiento = 'Tenés que aceptar el uso de tus datos'
    setErrores(v)
    if (Object.keys(v).length > 0) return
    setEnviando(true)
    try {
      const r = await enviarClientePotencial({ nombre: f.nombre, telefono: f.telefono, email: f.email, mensaje: f.mensaje, aceptaTratamiento: f.acepta, codigoPropiedad }, 'CHAT')
      setEnviado(r.mensaje)
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.campos).length > 0) {
        const { interes, ...resto } = err.campos
        setErrores(interes ? { ...resto, mensaje: interes } : resto)
      } else {
        setGeneral(err instanceof ApiError ? err.message : 'No se pudo enviar. Intentá de nuevo en unos minutos.')
      }
    } finally {
      setEnviando(false)
    }
  }

  if (enviado) return <div className={styles.contactoExito} role="status">{enviado}</div>
  const err = (k: string) => errores[k] && <span className={styles.contactoError}>{errores[k]}</span>

  return (
    <form className={styles.contactoForm} onSubmit={enviar} noValidate aria-label="Dejar mis datos">
      {general && <div className={styles.contactoError} role="alert">{general}</div>}
      <label className={styles.contactoCampo}><span>Nombre</span>
        <input className={styles.input} maxLength={120} autoComplete="name" value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} />{err('nombre')}</label>
      <label className={styles.contactoCampo}><span>Teléfono</span>
        <input className={styles.input} type="tel" maxLength={30} autoComplete="tel" value={f.telefono} onChange={(e) => setF({ ...f, telefono: e.target.value })} />{err('telefono')}</label>
      <label className={styles.contactoCampo}><span>Email</span>
        <input className={styles.input} type="email" maxLength={120} autoComplete="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />{err('email')}</label>
      {err('contacto')}
      <label className={styles.contactoCampo}><span>Mensaje</span>
        <textarea className={styles.input} rows={3} maxLength={1000} value={f.mensaje} onChange={(e) => setF({ ...f, mensaje: e.target.value })} />{err('mensaje')}</label>
      <label className={styles.contactoAcepta}>
        <input type="checkbox" checked={f.acepta} onChange={(e) => setF({ ...f, acepta: e.target.checked })} />
        <span>{TEXTO_ACEPTACION}</span>
      </label>
      {err('aceptaTratamiento')}
      <button type="submit" className={styles.contactoEnviar} disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar mis datos'}</button>
    </form>
  )
}

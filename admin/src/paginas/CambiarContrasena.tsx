import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import BotonVolver from '../componentes/BotonVolver'
import { api } from '../api/cliente'
import { useSesion } from '../auth/Sesion'
import { erroresDe } from '../componentes/errores'
import ui from '../ui.module.css'
import CampoContrasena from '../componentes/CampoContrasena'

/** Cambio de la contraseña propia. Obligatorio si la cuenta tiene una contraseña temporal. */
export default function CambiarContrasena() {
  const { usuario, recargar } = useSesion()
  const navegar = useNavigate()
  const obligatorio = usuario?.debeCambiarPassword ?? false
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetida, setRepetida] = useState('')
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setCampos({})
    setGeneral(null)
    if (nueva !== repetida) {
      setCampos({ repetida: 'Las contraseñas no coinciden' })
      return
    }
    setGuardando(true)
    try {
      await api.put('/api/admin/cuenta/contrasena', { passwordActual: actual, passwordNueva: nueva })
      await recargar()
      navegar('/cuenta', { replace: true, state: { mensaje: 'Contraseña actualizada.' } })
    } catch (err) {
      const r = erroresDe(err)
      setCampos(r.campos)
      setGeneral(r.general)
    } finally {
      setGuardando(false)
    }
  }

  const err = (c: string) => campos[c] && <span className={ui.errorCampo}>{campos[c]}</span>

  return (
    <>
      <div className={ui.tituloPagina}>
        <div className={ui.tituloConVolver}>{!obligatorio && <BotonVolver alternativa="/cuenta" />}<h1>Cambiar contraseña</h1></div>
      </div>
      {obligatorio && (
        <div className={ui.aviso}>Su cuenta tiene una contraseña temporal. Debe cambiarla para continuar usando el panel.</div>
      )}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}
      <form onSubmit={guardar} noValidate>
        <section className={ui.tarjeta}>
          <h2>Contraseñas</h2>
          <div className={ui.grilla}>
            <label className={ui.campo}>
              <span className={ui.etiqueta}>Contraseña actual</span>
              <CampoContrasena autoComplete="current-password" value={actual} onChange={(e) => setActual(e.target.value)} />
              {err('passwordActual')}
            </label>
            <label className={ui.campo}>
              <span className={ui.etiqueta}>Nueva contraseña</span>
              <CampoContrasena autoComplete="new-password" value={nueva} onChange={(e) => setNueva(e.target.value)} />
              <span className={ui.ayuda}>Mínimo 10 caracteres, con al menos una letra y un número.</span>
              {err('passwordNueva')}
            </label>
            <label className={ui.campo}>
              <span className={ui.etiqueta}>Repetir la nueva contraseña</span>
              <CampoContrasena autoComplete="new-password" value={repetida} onChange={(e) => setRepetida(e.target.value)} />
              {err('repetida')}
            </label>
          </div>
        </section>
        <div className={ui.acciones}>
          <button type="submit" className={ui.boton} disabled={guardando}>{guardando ? 'Guardando…' : 'Cambiar contraseña'}</button>
        </div>
      </form>
    </>
  )
}

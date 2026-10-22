import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { useSesion } from '../auth/Sesion'
import ui from '../ui.module.css'
import CampoContrasena from '../componentes/CampoContrasena'

export default function Login() {
  const { usuario, cargando, expirada, iniciar } = useSesion()
  const navegar = useNavigate()
  // Al ingresar siempre se llega a Inicio (no a la última pantalla). Con contraseña temporal,
  // RutaProtegida lleva primero a cambiarla.
  const desde = '/'
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (!cargando && usuario) return <Navigate to={desde} replace />

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await iniciar(email.trim(), password)
      navegar(desde, { replace: true })
    } catch (err) {
      // 401: "Email o contraseña incorrectos" · 429: bloqueo por intentos · 400: campos vacíos
      if (err instanceof ApiError) {
        const detalle = Object.values(err.campos)
        setError(detalle.length ? detalle.join('. ') : err.message)
      } else {
        setError('No se pudo iniciar sesión.')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className={ui.loginFondo}>
      <div className={ui.loginCaja}>
        <div className={ui.loginMarca}>
          <span className={ui.marcaInsignia}>S&N</span>
          <div>
            <strong>Inmobiliaria</strong>
            <span>Panel administrativo</span>
          </div>
        </div>
        {expirada && <div className={ui.aviso}>Su sesión expiró. Vuelva a iniciar sesión.</div>}
        {error && (
          <div className={ui.avisoError} role="alert">
            {error}
          </div>
        )}
        <form onSubmit={enviar}>
          <label className={ui.campo}>
            <span className={ui.etiqueta}>Email</span>
            <input className={ui.input} type="email" autoComplete="username" value={email}
              onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className={ui.campo}>
            <span className={ui.etiqueta}>Contraseña</span>
            <CampoContrasena autoComplete="current-password" value={password}
              onChange={(e) => setPassword(e.target.value)} required />
          </label>
          <button className={ui.boton} type="submit" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
      </div>
    </div>
  )
}

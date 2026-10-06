import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/cliente'
import { useSesion } from '../auth/Sesion'
import ui from '../ui.module.css'

export default function Login() {
  const { usuario, cargando, expirada, iniciar } = useSesion()
  const navegar = useNavigate()
  const desde = (useLocation().state as { desde?: string } | null)?.desde ?? '/propiedades'
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
        <h1>S&N Inmobiliaria</h1>
        <p>Panel administrativo</p>
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
            <input className={ui.input} type="password" autoComplete="current-password" value={password}
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

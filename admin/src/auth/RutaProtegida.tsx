import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useSesion } from './Sesion'
import ui from '../ui.module.css'

/**
 * Redirige al login si no hay sesión. Es solo una comodidad de navegación: aunque alguien
 * la saltee, el backend rechaza con 401 toda operación de /api/admin/* sin sesión válida.
 */
export default function RutaProtegida() {
  const { usuario, cargando } = useSesion()
  const ubicacion = useLocation()

  if (cargando) return <p className={ui.cargando}>Verificando sesión…</p>
  if (!usuario) return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />
  // Con una contraseña temporal, solo se puede cambiarla (el backend bloquea todo lo demás)
  if (usuario.debeCambiarPassword && ubicacion.pathname !== '/cuenta/contrasena') {
    return <Navigate to="/cuenta/contrasena" replace />
  }
  return <Outlet />
}

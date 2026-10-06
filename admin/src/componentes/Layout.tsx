import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useSesion } from '../auth/Sesion'
import ui from '../ui.module.css'

export default function Layout() {
  const { usuario, cerrar } = useSesion()
  const navegar = useNavigate()

  async function salir() {
    await cerrar()
    navegar('/login', { replace: true })
  }

  return (
    <>
      <header className={ui.barra}>
        <span className={ui.marca}>
          S&N Inmobiliaria<small>Panel administrativo</small>
        </span>
        <NavLink to="/propiedades" end className={({ isActive }) => `${ui.navLink} ${isActive ? ui.navLinkActivo : ''}`}>
          Propiedades
        </NavLink>
        <NavLink to="/propiedades/nueva" className={({ isActive }) => `${ui.navLink} ${isActive ? ui.navLinkActivo : ''}`}>
          Nueva propiedad
        </NavLink>
        <span className={ui.usuario}>
          {usuario?.nombre} {usuario?.apellido}
        </span>
        <button className={ui.botonLink} onClick={salir}>
          Cerrar sesión
        </button>
      </header>
      <main className={ui.contenido}>
        <Outlet />
      </main>
    </>
  )
}

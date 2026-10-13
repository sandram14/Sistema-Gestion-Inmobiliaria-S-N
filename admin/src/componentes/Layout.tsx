import { Suspense, useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Building2, LogOut, Menu, MessagesSquare, Plus, UserRound, Users, X } from 'lucide-react'
import { useSesion } from '../auth/Sesion'
import { nombreDelRol } from './formato'
import ui from '../ui.module.css'

/**
 * Estructura del panel: menú lateral fijo en escritorio; en pantallas angostas el menú
 * se abre con un botón. El menú solo muestra secciones que ya funcionan.
 */
export default function Layout() {
  const { usuario, cerrar, puede } = useSesion()
  const navegar = useNavigate()
  const { pathname } = useLocation()
  const [menuAbierto, setMenuAbierto] = useState(false)

  // Al navegar, el menú se cierra (en pantallas angostas)
  useEffect(() => setMenuAbierto(false), [pathname])

  async function salir() {
    await cerrar()
    navegar('/login', { replace: true })
  }

  const enlace = ({ isActive }: { isActive: boolean }) => `${ui.menuItem} ${isActive ? ui.menuItemActivo : ''}`

  return (
    <div className={ui.shell}>
      <aside className={`${ui.lateral} ${menuAbierto ? ui.lateralAbierto : ''}`} aria-label="Menú del panel">
        <div className={ui.marca}>
          <span className={ui.marcaInsignia}>S&N</span>
          <span className={ui.marcaTexto}>
            <strong>Inmobiliaria</strong>
            <span>Panel administrativo</span>
          </span>
        </div>

        <nav className={ui.menu}>
          <div className={ui.menuGrupo}>
            <div className={ui.menuTitulo}>Propiedades</div>
            {puede('PROPIEDADES_VER') && (
              <NavLink to="/propiedades" end className={enlace}>
                <Building2 size={18} aria-hidden="true" /> Listado
              </NavLink>
            )}
            {puede('PROPIEDADES_EDITAR') && (
              <NavLink to="/propiedades/nueva" className={enlace}>
                <Plus size={18} aria-hidden="true" /> Nueva propiedad
              </NavLink>
            )}
          </div>
          {puede('CONSULTAS_VER') && (
            <div className={ui.menuGrupo}>
              <div className={ui.menuTitulo}>Clientes</div>
              <NavLink to="/consultas" className={enlace}>
                <MessagesSquare size={18} aria-hidden="true" /> Consultas IA
              </NavLink>
            </div>
          )}
          {puede('USUARIOS_VER') && (
            <div className={ui.menuGrupo}>
              <div className={ui.menuTitulo}>Administración</div>
              <NavLink to="/usuarios" className={enlace}>
                <Users size={18} aria-hidden="true" /> Usuarios
              </NavLink>
            </div>
          )}
          <div className={ui.menuGrupo}>
            <div className={ui.menuTitulo}>Cuenta</div>
            <NavLink to="/cuenta" end className={enlace}>
              <UserRound size={18} aria-hidden="true" /> Mi cuenta
            </NavLink>
          </div>
        </nav>

        <div className={ui.lateralPie}>
          <div className={ui.lateralUsuario}>
            <strong>{usuario?.nombre} {usuario?.apellido}</strong>
            <span>{nombreDelRol(usuario?.rol)}</span>
          </div>
          <button className={ui.botonSalir} onClick={salir}>
            <LogOut size={16} aria-hidden="true" /> Cerrar sesión
          </button>
        </div>
      </aside>

      {menuAbierto && <div className={ui.fondoMenu} onClick={() => setMenuAbierto(false)} aria-hidden="true" />}

      <div className={ui.principal}>
        <header className={ui.barraMovil}>
          <button
            className={ui.botonMenu}
            onClick={() => setMenuAbierto((a) => !a)}
            aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={menuAbierto}
          >
            {menuAbierto ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
          </button>
          <strong>S&N Inmobiliaria</strong>
        </header>
        <main className={ui.contenido}>
          <Suspense fallback={<p className={ui.cargando}>Cargando…</p>}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  )
}

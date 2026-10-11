import { useNavigate } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useSesion } from '../auth/Sesion'
import { nombreDelRol } from '../componentes/formato'
import ui from '../ui.module.css'

/** Datos del usuario conectado, tal como los informa el backend (GET /api/admin/auth/me). */
export default function MiCuenta() {
  const { usuario, cerrar } = useSesion()
  const navegar = useNavigate()

  async function salir() {
    await cerrar()
    navegar('/login', { replace: true })
  }

  return (
    <>
      <div className={ui.tituloPagina}>
        <h1>Mi cuenta</h1>
        <button className={ui.botonSecundario} onClick={salir}>
          <LogOut size={16} aria-hidden="true" /> Cerrar sesión
        </button>
      </div>

      <section className={ui.tarjeta}>
        <h2>Datos personales</h2>
        <dl className={ui.datos}>
          <dt>Nombre</dt>
          <dd>{usuario?.nombre}</dd>
          <dt>Apellido</dt>
          <dd>{usuario?.apellido}</dd>
          <dt>Email</dt>
          <dd>{usuario?.email}</dd>
          <dt>Rol</dt>
          <dd>{nombreDelRol(usuario?.rol)}</dd>
        </dl>
      </section>
    </>
  )
}

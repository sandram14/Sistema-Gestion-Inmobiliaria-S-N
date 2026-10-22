import { Outlet } from 'react-router-dom'
import type { Permiso } from '../api/tipos'
import { useSesion } from './Sesion'
import ui from '../ui.module.css'

/**
 * Muestra un aviso en lugar de la pantalla si el usuario no tiene el permiso (por ejemplo, un
 * vendedor que escribe /sitio/inicio en la dirección). Igual que RutaProtegida, es una comodidad:
 * el backend rechaza con 403 toda operación sin el permiso.
 */
export default function RutaConPermiso({ permiso, aviso }: { permiso: Permiso; aviso: string }) {
  const { puede } = useSesion()
  if (!puede(permiso)) return <div className={ui.avisoError} role="alert">{aviso}</div>
  return <Outlet />
}

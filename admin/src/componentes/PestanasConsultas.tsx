import { NavLink } from 'react-router-dom'
import { useSesion } from '../auth/Sesion'
import ui from '../ui.module.css'

/** Barra de pestañas de "Consultas IA". El vendedor solo ve Historial (el backend también lo restringe). */
export default function PestanasConsultas() {
  const { puede } = useSesion()
  const clase = ({ isActive }: { isActive: boolean }) => `${ui.pestana} ${isActive ? ui.pestanaActiva : ''}`
  return (
    <>
      <div className={ui.tituloPagina}><h1>Consultas IA</h1></div>
      <nav className={ui.pestanas} aria-label="Secciones de Consultas IA">
        <NavLink to="/consultas" end className={clase}>Historial</NavLink>
        {puede('CONSULTAS_VER_TODAS') && <NavLink to="/consultas/analisis" className={clase}>Análisis técnico</NavLink>}
        {puede('CONSULTAS_VER_TODAS') && <NavLink to="/consultas/calidad" className={clase}>Calidad de respuestas</NavLink>}
        {puede('CLIENTES_VER') && <NavLink to="/consultas/clientes" className={clase}>Clientes potenciales</NavLink>}
      </nav>
    </>
  )
}

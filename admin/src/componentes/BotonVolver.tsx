import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import ui from '../ui.module.css'

/**
 * Vuelve a la pantalla anterior (como el botón "atrás" del navegador).
 * Si la página se abrió directamente (no hay pantalla anterior dentro del panel), va a `alternativa`.
 */
export default function BotonVolver({ alternativa }: { alternativa: string }) {
  const navegar = useNavigate()
  const { key } = useLocation()
  // React Router marca con "default" la primera ubicación: no hay a dónde volver dentro del panel
  const hayAnterior = key !== 'default'
  return (
    <button type="button" className={ui.botonVolver} onClick={() => (hayAnterior ? navegar(-1) : navegar(alternativa))} aria-label="Volver a la pantalla anterior" title="Volver">
      <ArrowLeft size={18} aria-hidden="true" />
    </button>
  )
}

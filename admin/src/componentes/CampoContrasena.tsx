import { useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import ui from '../ui.module.css'

/**
 * Campo de contraseña con el botón del ojo para mostrarla u ocultarla. Solo cambia el tipo del campo
 * (password ↔ text): el valor no se toca. Empieza siempre oculta.
 */
export default function CampoContrasena({ className, ...props }: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [visible, setVisible] = useState(false)
  const accion = visible ? 'Ocultar contraseña' : 'Mostrar contraseña'
  return (
    <span className={ui.contrasena}>
      <input {...props} className={`${ui.input} ${className ?? ''}`} type={visible ? 'text' : 'password'} />
      <button type="button" className={ui.botonOjo} onClick={() => setVisible((v) => !v)} aria-label={accion} title={accion}>
        {visible ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
      </button>
    </span>
  )
}

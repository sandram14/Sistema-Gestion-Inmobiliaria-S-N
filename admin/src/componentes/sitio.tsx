import { useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { api } from '../api/cliente'
import { erroresDe } from './errores'
import ui from '../ui.module.css'

/** Barra de pestañas de "Gestión del sitio web" (ADMIN y GERENTE). */
export function PestanasSitio() {
  const clase = ({ isActive }: { isActive: boolean }) => `${ui.pestana} ${isActive ? ui.pestanaActiva : ''}`
  return (
    <>
      <div className={ui.tituloPagina}><h1>Gestión del sitio web</h1></div>
      <nav className={ui.pestanas} aria-label="Secciones del sitio web">
        <NavLink to="/sitio/inicio" className={clase}>Inicio</NavLink>
        <NavLink to="/sitio/ofertas" className={clase}>Ofertas</NavLink>
        <NavLink to="/sitio/contacto" className={clase}>Contacto</NavLink>
      </nav>
      <p className={ui.ayuda}>Los cambios se publican en la web al guardar (pueden tardar hasta un minuto en verse).</p>
    </>
  )
}

const TIPOS = ['image/jpeg', 'image/png', 'image/webp']

/** Imagen del sitio: vista previa, subir o reemplazar, y quitar (vuelve a la imagen del diseño). */
export function ImagenSitio({ etiqueta, valor, onCambio }: { etiqueta: string; valor: string | null; onCambio: (url: string | null) => void }) {
  const entrada = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const [subiendo, setSubiendo] = useState(false)

  async function subir(archivo: File | undefined) {
    setError(null)
    if (!archivo) return
    if (!TIPOS.includes(archivo.type)) { setError('Formato no admitido. Use imágenes JPEG, PNG o WebP.'); return }
    if (archivo.size > 5 * 1024 * 1024) { setError('La imagen supera el máximo de 5 MB.'); return }
    const datos = new FormData()
    datos.append('archivo', archivo)
    setSubiendo(true)
    try { onCambio((await api.subir<{ url: string }>('/api/admin/sitio/imagenes', datos)).url) } catch (e) { setError(erroresDe(e).general) }
    finally { setSubiendo(false); if (entrada.current) entrada.current.value = '' }
  }

  return (
    <div className={ui.campo}>
      <span className={ui.etiqueta}>{etiqueta}</span>
      {valor ? <img src={valor} alt="" className={ui.imagenSitio} /> : <div className={ui.imagenVacia}>Se usa la imagen del diseño actual</div>}
      <div className={ui.filaControl}>
        <input ref={entrada} type="file" accept={TIPOS.join(',')} hidden aria-label={`Archivo para ${etiqueta}`} onChange={(e) => void subir(e.target.files?.[0])} />
        <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={subiendo} onClick={() => entrada.current?.click()}>
          {subiendo ? 'Subiendo…' : valor ? 'Cambiar imagen' : 'Subir imagen'}
        </button>
        {valor && <button type="button" className={`${ui.botonPeligro} ${ui.botonChico}`} onClick={() => onCambio(null)}>Quitar</button>}
      </div>
      <span className={ui.ayuda}>JPEG, PNG o WebP; máximo 5 MB. Se aplica al guardar.</span>
      {error && <span className={ui.errorCampo}>{error}</span>}
    </div>
  )
}

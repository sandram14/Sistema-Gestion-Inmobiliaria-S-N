import { useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { api } from '../api/cliente'
import { erroresDe } from './errores'
import BotonVolver from './BotonVolver'
import ui from '../ui.module.css'

/** Barra de pestañas de "Gestión del sitio web" (ADMIN y GERENTE). */
export function PestanasSitio() {
  const clase = ({ isActive }: { isActive: boolean }) => `${ui.pestana} ${isActive ? ui.pestanaActiva : ''}`
  return (
    <>
      <div className={ui.tituloPagina}><div className={ui.tituloConVolver}><BotonVolver alternativa="/propiedades" /><h1>Gestión del sitio web</h1></div></div>
      <nav className={ui.pestanas} aria-label="Secciones del sitio web">
        <NavLink to="/sitio/inicio" className={clase}>Inicio</NavLink>
        <NavLink to="/sitio/nosotros" className={clase}>Nosotros</NavLink>
        <NavLink to="/sitio/contacto" className={clase}>Contacto</NavLink>
      </nav>
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

/** Editor de una lista de elementos con título y texto: editar, agregar, quitar y ordenar. */
export function ListaItems({ items, onCambio, min, max, prefijo, errores, nombre }: {
  items: { titulo: string; texto: string }[]; onCambio: (l: { titulo: string; texto: string }[]) => void
  min: number; max: number; prefijo: string; errores: Record<string, string>; nombre: string
}) {
  const cambiar = (i: number, k: 'titulo' | 'texto', v: string) => onCambio(items.map((x, j) => (j === i ? { ...x, [k]: v } : x)))
  const mover = (i: number, d: number) => { const n = [...items]; const [x] = n.splice(i, 1); n.splice(i + d, 0, x); onCambio(n) }
  return (
    <div className={ui.listaItems}>
      {items.map((it, i) => (
        <div key={i} className={ui.itemEditable}>
          <div className={ui.grilla}>
            <label className={ui.campo}><span className={ui.etiqueta}>{nombre} {i + 1}: título</span>
              <input className={ui.input} maxLength={60} value={it.titulo} onChange={(e) => cambiar(i, 'titulo', e.target.value)} />
              {errores[`${prefijo}[${i}].titulo`] && <span className={ui.errorCampo}>{errores[`${prefijo}[${i}].titulo`]}</span>}</label>
            <label className={`${ui.campo} ${ui.campoAncho}`}><span className={ui.etiqueta}>{nombre} {i + 1}: texto</span>
              <textarea className={ui.input} rows={2} maxLength={300} value={it.texto} onChange={(e) => cambiar(i, 'texto', e.target.value)} />
              {errores[`${prefijo}[${i}].texto`] && <span className={ui.errorCampo}>{errores[`${prefijo}[${i}].texto`]}</span>}</label>
          </div>
          <div className={ui.acciones}>
            <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={i === 0} onClick={() => mover(i, -1)}>Subir</button>
            <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={i === items.length - 1} onClick={() => mover(i, 1)}>Bajar</button>
            <button type="button" className={`${ui.botonPeligro} ${ui.botonChico}`} disabled={items.length <= min} onClick={() => onCambio(items.filter((_, j) => j !== i))}>Quitar</button>
          </div>
        </div>
      ))}
      {errores[prefijo] && <span className={ui.errorCampo}>{errores[prefijo]}</span>}
      <div className={ui.filaControl}>
        <button type="button" className={ui.botonSecundario} disabled={items.length >= max} onClick={() => onCambio([...items, { titulo: '', texto: '' }])}>Agregar {nombre.toLowerCase()}</button>
        <span className={ui.secundario}>{items.length} de {max} como máximo</span>
      </div>
    </div>
  )
}

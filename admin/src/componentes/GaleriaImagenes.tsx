import { useRef, useState } from 'react'
import { api, ApiError } from '../api/cliente'
import type { ImagenDto, PropiedadDetalle } from '../api/tipos'
import ui from '../ui.module.css'

const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png', 'image/webp']
const MAXIMO_BYTES = 5 * 1024 * 1024

/**
 * Fotos de la propiedad. El panel hace una comprobación previa (tipo y 5 MB) solo para
 * avisar antes de subir; la validación definitiva la hace el backend, que reconoce el
 * formato por el contenido del archivo y rechaza lo que no sea JPEG, PNG o WebP.
 */
export default function GaleriaImagenes({
  idPropiedad,
  imagenes,
  onActualizado,
  soloLectura = false,
}: {
  idPropiedad: number
  imagenes: ImagenDto[]
  onActualizado: (detalle: PropiedadDetalle) => void
  /** Sin acciones (por ejemplo, un vendedor viendo una propiedad ajena) */
  soloLectura?: boolean
}) {
  const entrada = useRef<HTMLInputElement>(null)
  const [archivo, setArchivo] = useState<File | null>(null)
  const [descripcion, setDescripcion] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const base = `/api/admin/propiedades/${idPropiedad}/imagenes`

  async function ejecutar(accion: () => Promise<PropiedadDetalle>) {
    setOcupado(true)
    setError(null)
    try {
      onActualizado(await accion())
    } catch (e) {
      if (e instanceof ApiError) {
        const detalle = Object.values(e.campos)
        setError(detalle.length ? detalle.join('. ') : e.message)
      } else {
        setError('No se pudo completar la operación.')
      }
    } finally {
      setOcupado(false)
    }
  }

  async function subir() {
    if (!archivo) return
    if (!TIPOS_PERMITIDOS.includes(archivo.type)) {
      setError('Formato no admitido. Use imágenes JPEG, PNG o WebP.')
      return
    }
    if (archivo.size > MAXIMO_BYTES) {
      setError('La imagen supera el máximo de 5 MB.')
      return
    }
    const datos = new FormData()
    datos.append('archivo', archivo)
    if (descripcion.trim()) datos.append('descripcion', descripcion.trim())
    await ejecutar(() => api.subir<PropiedadDetalle>(base, datos))
    setArchivo(null)
    setDescripcion('')
    if (entrada.current) entrada.current.value = ''
  }

  return (
    <div>
      {error && <div className={ui.avisoError} role="alert">{error}</div>}

      {imagenes.length === 0 ? (
        <p className={ui.ayuda}>Todavía no hay fotos. La primera que suba quedará como principal.</p>
      ) : (
        <div className={ui.galeria}>
          {imagenes.map((img) => (
            <figure key={img.id} className={`${ui.foto} ${img.esPrincipal ? ui.fotoPrincipal : ''}`} style={{ margin: 0 }}>
              <img src={img.url} alt={img.descripcion ?? ''} />
              {!soloLectura && <div className={ui.fotoAcciones}>
                {img.esPrincipal ? (
                  <span className={ui.estado}>Principal</span>
                ) : (
                  <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} disabled={ocupado}
                    onClick={() => ejecutar(() => api.patch<PropiedadDetalle>(`${base}/${img.id}/principal`))}>
                    Hacer principal
                  </button>
                )}
                <button type="button" className={`${ui.botonPeligro} ${ui.botonChico}`} disabled={ocupado}
                  onClick={() => {
                    if (window.confirm('¿Eliminar esta foto?')) void ejecutar(() => api.delete<PropiedadDetalle>(`${base}/${img.id}`))
                  }}>
                  Eliminar
                </button>
              </div>}
            </figure>
          ))}
        </div>
      )}

      {!soloLectura && <div className={ui.subida}>
        <label className={ui.campo}>
          <span className={ui.etiqueta}>Nueva foto (JPEG, PNG o WebP, hasta 5 MB)</span>
          <input ref={entrada} className={ui.input} type="file" accept="image/jpeg,image/png,image/webp"
            aria-label="Archivo de imagen" onChange={(e) => setArchivo(e.target.files?.[0] ?? null)} />
        </label>
        <label className={ui.campo}>
          <span className={ui.etiqueta}>Descripción (opcional)</span>
          <input className={ui.input} maxLength={200} value={descripcion} onChange={(e) => setDescripcion(e.target.value)} />
        </label>
        <button type="button" className={ui.boton} disabled={!archivo || ocupado} onClick={() => void subir()}>
          {ocupado ? 'Procesando…' : 'Subir foto'}
        </button>
      </div>}
    </div>
  )
}

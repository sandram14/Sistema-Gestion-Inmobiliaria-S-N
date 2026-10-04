// Ayudante para llamar a la API Java del backend.
//
// En desarrollo, Vite reenvía /api y /uploads a Tomcat (ver vite.config.ts), así que el
// navegador ve un único origen: no hay problemas de CORS y la cookie del asistente funciona.

/** Error de la API con el formato del backend: { error: { codigo, mensaje, campos } } */
export class ApiError extends Error {
  readonly status: number
  readonly codigo: string
  readonly campos: Record<string, string>

  constructor(status: number, codigo: string, mensaje: string, campos: Record<string, string> = {}) {
    super(mensaje)
    this.name = 'ApiError'
    this.status = status
    this.codigo = codigo
    this.campos = campos
  }

  /** Mensaje para mostrar al visitante (los de validación incluyen el detalle del campo). */
  mensajeParaVisitante(): string {
    const detalle = Object.values(this.campos)
    return detalle.length ? detalle.join('. ') : this.message
  }
}

const SIN_CONEXION =
  'No se pudo conectar con el servidor. Verificá tu conexión e intentá nuevamente en unos minutos.'

/** Arma "?a=1&b=x" omitiendo los valores vacíos (null, undefined o ''). */
export function construirQuery(params: Record<string, string | number | null | undefined>): string {
  const partes = Object.entries(params)
    .filter(([, v]) => v !== null && v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
  return partes.length ? `?${partes.join('&')}` : ''
}

async function pedir<T>(path: string, init: RequestInit, timeoutMs: number, signal?: AbortSignal): Promise<T> {
  const control = new AbortController()
  const temporizador = setTimeout(() => control.abort(), timeoutMs)
  const cancelarExterno = () => control.abort()
  signal?.addEventListener('abort', cancelarExterno)

  try {
    let res: Response
    try {
      res = await fetch(path, { ...init, signal: control.signal, credentials: 'same-origin' })
    } catch (e) {
      if (signal?.aborted) throw e // cancelada por quien llamó (por ejemplo, cambió un filtro)
      throw new ApiError(0, 'SIN_CONEXION', SIN_CONEXION)
    }

    if (res.ok) {
      return (res.status === 204 ? undefined : await res.json()) as T
    }
    let cuerpo: { error?: { codigo?: string; mensaje?: string; campos?: Record<string, string> } } = {}
    try {
      cuerpo = await res.json()
    } catch {
      // respuesta sin JSON (por ejemplo, el backend no está levantado y responde el proxy)
    }
    const error = cuerpo.error
    if (!error) throw new ApiError(res.status, 'SIN_CONEXION', SIN_CONEXION)
    throw new ApiError(res.status, error.codigo ?? 'ERROR', error.mensaje ?? 'Ocurrió un error inesperado.', error.campos)
  } finally {
    clearTimeout(temporizador)
    signal?.removeEventListener('abort', cancelarExterno)
  }
}

export function apiGet<T>(
  path: string,
  params: Record<string, string | number | null | undefined> = {},
  signal?: AbortSignal
): Promise<T> {
  return pedir<T>(`${path}${construirQuery(params)}`, { headers: { Accept: 'application/json' } }, 15000, signal)
}

/** POST con cuerpo JSON. El tiempo de espera es mayor porque el asistente puede demorar unos segundos. */
export function apiPost<T>(path: string, cuerpo: unknown, signal?: AbortSignal): Promise<T> {
  return pedir<T>(
    path,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(cuerpo),
    },
    30000,
    signal
  )
}

/** true si el error es solo porque se canceló la petición (no hay que mostrarlo). */
export function esCancelacion(e: unknown): boolean {
  return e instanceof DOMException && e.name === 'AbortError'
}

// Cliente HTTP del panel. La sesión vive en la cookie HttpOnly del backend: el panel
// nunca ve ni guarda credenciales ni tokens. Cada operación administrativa la vuelve a
// autorizar el backend (AuthFilter); la protección de rutas de React es solo para la
// navegación, no es una medida de seguridad.

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
}

/** Evento que se emite cuando el backend responde 401 (sesión expirada o cerrada). */
export const EVENTO_SESION_EXPIRADA = 'sn-admin:sesion-expirada'

const SIN_CONEXION = 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.'

async function pedir<T>(metodo: string, path: string, cuerpo?: BodyInit, contentType?: string): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (contentType) headers['Content-Type'] = contentType

  let res: Response
  try {
    res = await fetch(path, { method: metodo, headers, body: cuerpo, credentials: 'same-origin' })
  } catch {
    throw new ApiError(0, 'SIN_CONEXION', SIN_CONEXION)
  }

  if (res.ok) {
    return (res.status === 204 ? undefined : await res.json()) as T
  }

  let datos: { error?: { codigo?: string; mensaje?: string; campos?: Record<string, string> } } = {}
  try {
    datos = await res.json()
  } catch {
    // sin cuerpo JSON (por ejemplo, el proxy no encuentra el backend)
  }
  const e = datos.error
  const error = e
    ? new ApiError(res.status, e.codigo ?? 'ERROR', e.mensaje ?? 'Ocurrió un error inesperado.', e.campos)
    : new ApiError(res.status, 'SIN_CONEXION', SIN_CONEXION)

  // Las rutas de login y "me" manejan su propio 401; en el resto significa sesión vencida
  if (res.status === 401 && !path.startsWith('/api/admin/auth/')) {
    window.dispatchEvent(new CustomEvent(EVENTO_SESION_EXPIRADA))
  }
  throw error
}

export const api = {
  get: <T>(path: string) => pedir<T>('GET', path),
  post: <T>(path: string, cuerpo?: unknown) =>
    pedir<T>('POST', path, cuerpo === undefined ? undefined : JSON.stringify(cuerpo), cuerpo === undefined ? undefined : 'application/json'),
  put: <T>(path: string, cuerpo: unknown) => pedir<T>('PUT', path, JSON.stringify(cuerpo), 'application/json'),
  patch: <T>(path: string, cuerpo?: unknown) =>
    pedir<T>('PATCH', path, cuerpo === undefined ? undefined : JSON.stringify(cuerpo), cuerpo === undefined ? undefined : 'application/json'),
  delete: <T>(path: string) => pedir<T>('DELETE', path),
  /** Subida de archivos (multipart). El navegador arma el Content-Type con su boundary. */
  subir: <T>(path: string, datos: FormData) => pedir<T>('POST', path, datos),
}

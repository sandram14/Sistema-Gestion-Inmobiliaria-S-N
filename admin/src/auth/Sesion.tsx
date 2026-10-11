import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, ApiError, EVENTO_SESION_EXPIRADA } from '../api/cliente'
import type { Permiso, UsuarioSesion } from '../api/tipos'

// Estado de la sesión en el navegador. La sesión real es la cookie HttpOnly del backend:
// aquí solo se recuerda quién inició sesión para mostrarlo y para la navegación.
// El backend vuelve a verificar la sesión en CADA operación administrativa.

interface Sesion {
  usuario: UsuarioSesion | null
  cargando: boolean
  /** true si la sesión venció mientras se usaba el panel */
  expirada: boolean
  iniciar: (email: string, password: string) => Promise<void>
  cerrar: () => Promise<void>
  /** Si el usuario tiene el permiso. Solo para mostrar u ocultar opciones: el backend lo vuelve a comprobar. */
  puede: (permiso: Permiso) => boolean
}

const ContextoSesion = createContext<Sesion | null>(null)

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null)
  const [cargando, setCargando] = useState(true)
  const [expirada, setExpirada] = useState(false)

  // Al abrir el panel: ¿hay una sesión activa en el backend?
  useEffect(() => {
    api
      .get<{ usuario: UsuarioSesion }>('/api/admin/auth/me')
      .then((r) => setUsuario(r.usuario))
      .catch(() => setUsuario(null))
      .finally(() => setCargando(false))
  }, [])

  // Cualquier 401 del backend durante el uso → la sesión venció
  useEffect(() => {
    function alExpirar() {
      setUsuario(null)
      setExpirada(true)
    }
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
    return () => window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar)
  }, [])

  const iniciar = useCallback(async (email: string, password: string) => {
    const r = await api.post<{ usuario: UsuarioSesion }>('/api/admin/auth/login', { email, password })
    setUsuario(r.usuario)
    setExpirada(false)
  }, [])

  const puede = useCallback((permiso: Permiso) => usuario?.permisos?.includes(permiso) ?? false, [usuario])

  const cerrar = useCallback(async () => {
    try {
      await api.post('/api/admin/auth/logout')
    } catch (e) {
      if (!(e instanceof ApiError)) throw e // si el backend no responde, igual se sale del panel
    }
    setUsuario(null)
    setExpirada(false)
  }, [])

  return (
    <ContextoSesion.Provider value={{ usuario, cargando, expirada, iniciar, cerrar, puede }}>{children}</ContextoSesion.Provider>
  )
}

export function useSesion(): Sesion {
  const s = useContext(ContextoSesion)
  if (!s) throw new Error('useSesion debe usarse dentro de ProveedorSesion')
  return s
}

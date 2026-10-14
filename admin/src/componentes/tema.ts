/**
 * Tema del panel: claro, oscuro o el del sistema operativo.
 * La preferencia se guarda en este navegador (localStorage) y se aplica antes de mostrar el panel.
 */
import { useEffect, useState } from 'react'

export type Tema = 'claro' | 'oscuro' | 'sistema'
const CLAVE = 'sn-panel-tema'

export function temaGuardado(): Tema {
  try {
    const t = localStorage.getItem(CLAVE)
    return t === 'claro' || t === 'oscuro' ? t : 'sistema'
  } catch {
    return 'sistema'
  }
}

/** "sistema" quita el atributo: el CSS sigue a prefers-color-scheme. */
export function aplicarTema(t: Tema) {
  if (t === 'sistema') delete document.documentElement.dataset.tema
  else document.documentElement.dataset.tema = t
  try {
    if (t === 'sistema') localStorage.removeItem(CLAVE)
    else localStorage.setItem(CLAVE, t)
  } catch {
    /* sin almacenamiento: el tema dura hasta recargar */
  }
}

export function useTema(): [Tema, (t: Tema) => void] {
  const [tema, setTema] = useState<Tema>(temaGuardado)
  useEffect(() => aplicarTema(tema), [tema])
  return [tema, setTema]
}

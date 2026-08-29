import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

/**
 * Sin esto, React Router mantiene la posición de scroll entre páginas: si
 * hacés clic en una oferta estando abajo del todo, la página nueva "arranca"
 * donde quedaste en vez de arriba. Este componente lleva el scroll al tope
 * cada vez que cambia la ruta.
 */
export default function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

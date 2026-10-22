import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import styles from './Navbar.module.css'

const LINKS = [
  { label: 'Inicio', path: '/' },
  { label: 'Buscar', path: '/buscar' },
  { label: 'Nosotros', path: '/nosotros' },
  { label: 'Contacto', path: '/contacto' },
]

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 40)
    window.addEventListener('scroll', fn, { passive: true })
    return () => window.removeEventListener('scroll', fn)
  }, [])

  // Si cambia de página (por ej. al tocar un link), cerramos el menú mobile.
  useEffect(() => {
    setMobileOpen(false)
  }, [location.pathname])

  return (
    // Transparente solo sobre la portada oscura del inicio; en las demás páginas (algunas empiezan con
    // fondo claro, como Contacto) el encabezado tiene su fondo desde el principio para que se lea.
    <nav className={`${styles.nav} ${scrolled || mobileOpen || location.pathname !== '/' ? styles.navScrolled : ''}`}>
      <div className={styles.inner}>
        {/* Logo */}
        <div className={styles.logo}>
          <div className={styles.logoBadge}>
            <span className={styles.logoBadgeText}>S&N</span>
          </div>
          {/* La insignia ya dice "S&N": el texto completa la marca, "S&N Inmobiliaria" */}
          <span className={`${styles.logoText} ${styles.logoAccent}`}>Inmobiliaria</span>
        </div>

        {/* Links (desktop) */}
        <div className={styles.links}>
          {LINKS.map((link) => (
            <NavLink key={link.label} label={link.label} path={link.path} />
          ))}
        </div>

        {/* Botón hamburguesa (mobile) */}
        <button
          className={styles.hamburger}
          onClick={() => setMobileOpen((v) => !v)}
          aria-label={mobileOpen ? 'Cerrar menú' : 'Abrir menú'}
          aria-expanded={mobileOpen}
        >
          <span className={`${styles.hamburgerBar} ${mobileOpen ? styles.hamburgerBarTop : ''}`} />
          <span className={`${styles.hamburgerBar} ${mobileOpen ? styles.hamburgerBarHidden : ''}`} />
          <span className={`${styles.hamburgerBar} ${mobileOpen ? styles.hamburgerBarBottom : ''}`} />
        </button>
      </div>

      {/* Menú desplegable (mobile) */}
      <div className={`${styles.mobileMenu} ${mobileOpen ? styles.mobileMenuOpen : ''}`}>
        {LINKS.map((link) => (
          <NavLink key={link.label} label={link.label} path={link.path} mobile />
        ))}
      </div>
    </nav>
  )
}

function NavLink({
  label,
  path,
  mobile,
}: {
  label: string
  path: string
  mobile?: boolean
}) {
  const navigate = useNavigate()
  const location = useLocation()
  const isActive = location.pathname === path

  return (
    <a
      href={path}
      onClick={(e) => {
        e.preventDefault()
        navigate(path)
      }}
      className={`${mobile ? styles.mobileLink : styles.link} ${isActive ? styles.linkActive : ''}`}
    >
      {label}
    </a>
  )
}

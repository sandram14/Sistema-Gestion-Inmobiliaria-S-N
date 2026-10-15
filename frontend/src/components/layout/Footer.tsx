import { useNavigate } from 'react-router-dom'
import styles from './Footer.module.css'

const COLUMNAS: { title: string; links: { label: string; to?: string }[] }[] = [
  {
    title: 'Propiedades',
    links: [
      { label: 'Comprar', to: '/buscar?categoria=Vivienda&tipoOperacion=Venta' },
      { label: 'Alquilar', to: '/buscar?categoria=Vivienda&tipoOperacion=Alquiler' },
    ],
  },
  {
    title: 'Empresa',
    links: [
      { label: 'Nosotros', to: '/nosotros' },
      { label: 'Servicios', to: '/servicios' },
    ],
  },
  {
    title: 'Legal',
    links: [
      { label: 'Privacidad' },
      { label: 'Términos' },
      { label: 'Cookies' },
      { label: 'Contacto', to: '/contacto' },
    ],
  },
]

export default function Footer() {
  const navigate = useNavigate()

  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div className={styles.grid}>
          {/* Marca */}
          <div>
            <div className={styles.brandRow}>
              <div className={styles.brandBadge}>
                <span className={styles.brandBadgeText}>S&N</span>
              </div>
              {/* La insignia ya dice "S&N": el texto completa la marca, "S&N Inmobiliaria" */}
              <span className={`${styles.brandName} ${styles.brandAccent}`}>Inmobiliaria</span>
            </div>
            <p className={styles.brandText}>
              La plataforma inmobiliaria más avanzada de Alto Paraná para comprar, alquilar e
              invertir con confianza.
            </p>
          </div>

          {/* Columnas de links */}
          {COLUMNAS.map((col) => (
            <div key={col.title}>
              <div className={styles.colTitle}>{col.title}</div>
              <div className={styles.colLinks}>
                {col.links.map((link) => (
                  <a
                    key={link.label}
                    href={link.to ?? '#'}
                    className={styles.link}
                    style={{ cursor: link.to ? 'pointer' : 'default' }}
                    onClick={(e) => {
                      e.preventDefault()
                      if (link.to) navigate(link.to)
                    }}
                  >
                    {link.label}
                  </a>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Barra inferior */}
        <div className={styles.bottomBar}>
          <span className={styles.bottomText}>© 2026 S&N Inmobiliaria. Todos los derechos reservados.</span>
          <span className={styles.bottomText}>Ciudad del Este, Paraguay</span>
        </div>
      </div>
    </footer>
  )
}

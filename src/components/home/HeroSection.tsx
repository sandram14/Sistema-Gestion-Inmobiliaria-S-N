import { useNavigate } from 'react-router-dom'
import styles from './HeroSection.module.css'

export default function HeroSection() {
  const navigate = useNavigate()

  return (
    <section className={styles.hero}>
      <div className={styles.bgImage} />
      <div className={styles.bgOverlay} />
      <div className={styles.bgGlow} />

      <div className={styles.content}>
        <div className={styles.badge}>
          <span className={styles.badgeDot} />
          <span className={styles.badgeText}>Más de 12.000 propiedades disponibles</span>
        </div>

        <h1 className={styles.headline}>
          Encuentra el lugar donde
          <br />
          comenzará <em className={styles.headlineAccent}>tu próxima historia.</em>
        </h1>

        <p className={styles.subtitle}>
          S&N Inmobiliaria conecta compradores e inversores con las mejores propiedades, con
          tecnología que simplifica cada decisión.
        </p>

        <div className={styles.ctaRow}>
          <button className={styles.ctaBtn} onClick={() => navigate('/buscar')}>
            Buscar propiedades
          </button>
        </div>
      </div>

      <div className={styles.scrollIndicator}>
        <span className={styles.scrollLabel}>Explorar</span>
        <div className={styles.scrollLine} />
      </div>
    </section>
  )
}

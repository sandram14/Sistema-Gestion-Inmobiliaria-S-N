import { useNavigate } from 'react-router-dom'
import { destacar } from '../comun/destacar'
import { useContenidoSitio } from '../../services/contenidoSitio'
import styles from './HeroSection.module.css'

export default function HeroSection() {
  const navigate = useNavigate()
  const { contenido } = useContenidoSitio()
  const { imagenPortada, titulo, subtitulo } = contenido.inicio

  return (
    <section className={styles.hero}>
      {/* Imagen de portada cargada desde el panel; sin imagen, se ve el fondo del diseño */}
      <div className={styles.bgImage} style={imagenPortada ? { backgroundImage: `url("${imagenPortada}")` } : undefined} />
      <div className={styles.bgOverlay} />
      <div className={styles.bgGlow} />
      <div className={styles.content}>
        <h1 className={styles.headline}>{destacar(titulo, styles.headlineAccent)}</h1>
        {subtitulo && <p className={styles.subtitle}>{subtitulo}</p>}
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

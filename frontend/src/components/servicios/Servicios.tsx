import { useNavigate } from 'react-router-dom'
import { ImageCarousel } from '../busqueda/SearchFilter'
import styles from './Servicios.module.css'

const SERVICIOS = [
  {
    icon: '🏠',
    img: '/images/stock/compra-venta.jpg',
    title: 'Compra y Venta',
    text: 'Te asesoramos durante todo el proceso de adquisición o venta de inmuebles.',
  },
  {
    icon: '🔑',
    img: '/images/stock/alquiler.jpg',
    title: 'Alquileres',
    text: 'Encontrá la propiedad ideal para alquilar de forma rápida y confiable.',
  },
  {
    icon: '🌳',
    img: '/images/stock/terreno.jpg',
    title: 'Terrenos',
    text: 'Encontrá terrenos para construir, invertir o desarrollar tu próximo proyecto.',
  },
  {
    icon: '🤝',
    img: '/images/stock/asesoria.jpg',
    title: 'Asesoría Inmobiliaria',
    text: 'Te acompañamos para tomar decisiones seguras y encontrar la mejor opción según tus necesidades.',
  },
]

// Fotos reales de muestra (mezcla propiedades y terrenos, no solo terrenos).
const FOTOS_DESTACADAS = [
  '/images/propiedades/prop-001/1.png',
  '/images/terrenos/terr-001/1.png',
  '/images/propiedades/prop-003/1.png',
  '/images/terrenos/terr-003/1.png',
]

export default function Servicios() {
  const navigate = useNavigate()

  return (
    <>
      {/* 1. Intro */}
      <section className={styles.intro}>
        <span className={styles.eyebrow}>Servicios</span>
        <h1 className={styles.title}>Nuestros Servicios</h1>
        <p className={styles.introText}>Una solución para cada necesidad.</p>
      </section>

      {/* 2. Los 4 servicios */}
      <section className={styles.servicios}>
        <div className={styles.serviciosGrid}>
          {SERVICIOS.map((s) => (
            <div
           key={s.title}
           className={styles.servicioCard}
           style={{ backgroundImage: `url(${s.img})` }}
           >
            <h3 className={styles.servicioTitle}>{s.title}</h3>
            <p className={styles.servicioText}>{s.text}</p>
          </div>
          ))}
        </div>
      </section>

      {/* 3. Terrenos y propiedades + fotos deslizables */}
      <section className={styles.destacado}>
        <div className={styles.destacadoGrid}>
          <div>
            <span className={styles.eyebrowDark}>Lo que tenemos para vos</span>
            <h2 className={styles.h2}>
              Encontrá el terreno o la propiedad <em className={styles.h2Accent}>ideal para tu proyecto.</em>
            </h2>
            <p className={styles.body}>
              Contamos con terrenos y propiedades ubicados en diferentes zonas de Alto Paraná,
              ideales para vivienda, inversión o desarrollo comercial.
            </p>
          </div>
          <div className={styles.destacadoCarousel}>
            <ImageCarousel images={FOTOS_DESTACADAS} />
          </div>
        </div>
      </section>

      {/* 4. CTA */}
      <section className={styles.cta}>
        <h2 className={styles.ctaTitle}>¿Buscás tu próxima propiedad?</h2>
        <button className={styles.ctaBtn} onClick={() => navigate('/buscar')}>
          Ver propiedades →
        </button>
      </section>
    </>
  )
}

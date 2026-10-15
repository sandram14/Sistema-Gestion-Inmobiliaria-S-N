import { ArrowRight, Handshake, HeartHandshake, Home, Lightbulb, MapPinned, MessageCircle, Search, type LucideIcon } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import styles from './Nosotros.module.css'

const VALORES: { icon: LucideIcon; title: string; text: string }[] = [
  {
    icon: Handshake,
    title: 'Confianza',
    text: 'Construimos cada relación sobre información real y compromisos que cumplimos.',
  },
  {
    icon: Search,
    title: 'Transparencia',
    text: 'Sin letra chica: precios, estados y condiciones claras desde el primer contacto.',
  },
  {
    icon: HeartHandshake,
    title: 'Compromiso',
    text: 'Acompañamos todo el proceso, de la primera visita a la entrega de llaves.',
  },
  {
    icon: Lightbulb,
    title: 'Innovación',
    text: 'Herramientas como el mapa por barrios para que encuentres más rápido.',
  },
]

const RAZONES: { icon: LucideIcon; title: string; text: string }[] = [
  {
    icon: Home,
    title: 'Conocemos cada barrio',
    text: 'Cubrimos Alto Paraná zona por zona, con información actualizada de cada barrio.',
  },
  {
    icon: MapPinned,
    title: 'Búsqueda inteligente',
    text: 'Filtrá por mapa, precio y tipo de propiedad hasta encontrar justo lo que buscás.',
  },
  {
    icon: MessageCircle,
    title: 'Acompañamiento real',
    text: 'Un agente especializado te guía en cada paso, sin apuros ni letra chica.',
  },
]

export default function Nosotros() {
  const navigate = useNavigate()

  return (
    <>
      {/* 1. Sobre Nosotros — intro */}
      <section className={styles.intro}>
        <span className={styles.eyebrow}>S&N Inmobiliaria</span>
        <h1 className={styles.title}>Sobre Nosotros</h1>
        <p className={styles.introText}>
          Somos una plataforma enfocada en conectar a las personas con las mejores oportunidades
          inmobiliarias, brindando un servicio transparente, moderno y adaptado a tus necesidades.
        </p>
      </section>

      {/* 2. Quiénes somos */}
      <section className={styles.quienes}>
        <div className={styles.quienesGrid}>
          <img src="/images/stock/nosotros_page.jpg"  className={styles.quienesImg} />
          <div>
            <span className={styles.eyebrowDark}>Quiénes somos</span>
            <h2 className={styles.h2}>
              Un equipo que conoce <em className={styles.h2Accent}>Alto Paraná de cerca.</em>
            </h2>
            <p className={styles.body}>
              Nacimos en Ciudad del Este con un objetivo simple: que comprar, vender o alquilar una
              propiedad sea un proceso claro, sin sorpresas. Hoy acompañamos a familias e
              inversores en toda la región, barrio por barrio.
            </p>
            <div className={styles.quienesStats}>
              <div className={styles.quienesStat}>
                <span className={styles.quienesStatValue}>+150</span>
                <span className={styles.quienesStatLabel}>Propiedades</span>
              </div>
              <div className={styles.quienesStat}>
                <span className={styles.quienesStatValue}>+12</span>
                <span className={styles.quienesStatLabel}>Barrios cubiertos</span>
              </div>
              <div className={styles.quienesStat}>
                <span className={styles.quienesStatValue}>+6</span>
                <span className={styles.quienesStatLabel}>Años de experiencia</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Nuestros valores + Por qué elegirnos (combinado, lado a lado) */}
      <section className={styles.combinado}>
        <div className={styles.combinadoGrid}>
          <div>
            <span className={styles.eyebrowDark}>Nuestros valores</span>
            <h2 className={styles.h2}>
              Lo que nos <em className={styles.h2Accent}>guía todos los días.</em>
            </h2>
            <div className={styles.valoresGrid}>
              {VALORES.map((v) => (
                <div key={v.title} className={styles.valorCard}>
                  <span className={styles.valorIcon}><v.icon size={24} aria-hidden="true" /></span>
                  <h3 className={styles.valorTitle}>{v.title}</h3>
                  <p className={styles.valorText}>{v.text}</p>
                </div>
              ))}
            </div>
          </div>

          <div>
            <span className={styles.eyebrowDark}>Por qué elegirnos</span>
            <h2 className={styles.h2}>
              ¿Por qué elegir <em className={styles.h2Accent}>S&N?</em>
            </h2>
            <div className={styles.razonesStack}>
              {RAZONES.map((r) => (
                <div key={r.title} className={styles.razonRow}>
                  <span className={styles.razonIcon}><r.icon size={24} aria-hidden="true" /></span>
                  <div>
                    <h3 className={styles.razonTitle}>{r.title}</h3>
                    <p className={styles.razonText}>{r.text}</p>
                  </div>
                </div>
              ))}
            </div>
            <img
              src="/images/stock/nosotros_page2.jpg"
              className={styles.razonesImg}
            />

          </div>
        </div>
      </section>

      {/* 4. CTA */}
      <section className={styles.cta}>
        <h2 className={styles.ctaTitle}>¿Buscás tu próxima propiedad?</h2>
        <button className={styles.ctaBtn} onClick={() => navigate('/buscar')}>
          Ver propiedades <ArrowRight size={16} aria-hidden="true" />
        </button>
      </section>
    </>
  )
}

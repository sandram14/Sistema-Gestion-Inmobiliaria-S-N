import { ArrowRight, Handshake, HeartHandshake, Home, Lightbulb, MapPinned, MessageCircle, Search, type LucideIcon } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { destacar } from '../comun/destacar'
import { useContenidoSitio } from '../../services/contenidoSitio'
import styles from './Nosotros.module.css'

// Íconos asignados por posición: desde el panel se pueden cargar entre 1 y 6 valores y razones
const ICONOS_VALORES: LucideIcon[] = [Handshake, Search, HeartHandshake, Lightbulb]
const ICONOS_RAZONES: LucideIcon[] = [Home, MapPinned, MessageCircle]

export default function Nosotros() {
  const navigate = useNavigate()
  const n = useContenidoSitio().contenido.nosotros

  return (
    <>
      {/* 1. Sobre Nosotros — intro */}
      <section className={styles.intro}>
        <span className={styles.eyebrow}>S&N Inmobiliaria</span>
        <h1 className={styles.title}>Sobre Nosotros</h1>
        {n.introduccion && <p className={styles.introText}>{n.introduccion}</p>}
      </section>

      {/* 2. Quiénes somos */}
      <section className={styles.quienes}>
        <div className={styles.quienesGrid}>
          <img src={n.imagen ?? '/images/stock/nosotros_page.jpg'} alt="" className={styles.quienesImg} />
          <div>
            {n.historiaTitulo && <h2 className={styles.h2}>{destacar(n.historiaTitulo, styles.h2Accent)}</h2>}
            {n.historiaTexto && <p className={styles.body}>{n.historiaTexto}</p>}
            {n.cifras.length > 0 && (
              <div className={styles.quienesStats}>
                {n.cifras.map((c, k) => (
                  <div key={k} className={styles.quienesStat}>
                    <span className={styles.quienesStatValue}>{c.valor}</span>
                    <span className={styles.quienesStatLabel}>{c.etiqueta}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>

      {/* 3. Nuestros valores + Por qué elegirnos (combinado, lado a lado) */}
      <section className={styles.combinado}>
        <div className={styles.combinadoGrid}>
          <div>
            <h2 className={styles.h2}>
              Lo que nos <em className={styles.h2Accent}>guía todos los días.</em>
            </h2>
            <div className={styles.valoresGrid}>
              {n.valores.map((v, k) => {
                const Icono = ICONOS_VALORES[k % ICONOS_VALORES.length]
                return (
                  <div key={k} className={styles.valorCard}>
                    <span className={styles.valorIcon}><Icono size={24} aria-hidden="true" /></span>
                    <h3 className={styles.valorTitle}>{v.titulo}</h3>
                    <p className={styles.valorText}>{v.texto}</p>
                  </div>
                )
              })}
            </div>
          </div>

          <div>
            <h2 className={styles.h2}>
              ¿Por qué elegir <em className={styles.h2Accent}>S&N?</em>
            </h2>
            <div className={styles.razonesStack}>
              {n.razones.map((r, k) => {
                const Icono = ICONOS_RAZONES[k % ICONOS_RAZONES.length]
                return (
                  <div key={k} className={styles.razonRow}>
                    <span className={styles.razonIcon}><Icono size={24} aria-hidden="true" /></span>
                    <div>
                      <h3 className={styles.razonTitle}>{r.titulo}</h3>
                      <p className={styles.razonText}>{r.texto}</p>
                    </div>
                  </div>
                )
              })}
            </div>
            <img src="/images/stock/nosotros_page2.jpg" alt="" className={styles.razonesImg} />

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

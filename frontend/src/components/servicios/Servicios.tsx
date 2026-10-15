import { useEffect, useState } from 'react'
import { ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { ImageCarousel } from '../busqueda/SearchFilter'
import { apiGet } from '../../services/api'
import type { GeoFeatureCollection, PropiedadFeature } from '../PropertyMap/types'
import styles from './Servicios.module.css'

const SERVICIOS = [
  {
    img: '/images/stock/compra-venta.jpg',
    title: 'Compra y Venta',
    text: 'Te asesoramos durante todo el proceso de adquisición o venta de inmuebles.',
  },
  {
    img: '/images/stock/alquiler.jpg',
    title: 'Alquileres',
    text: 'Encontrá la propiedad ideal para alquilar de forma rápida y confiable.',
  },
  {
    img: '/images/stock/terreno.jpg',
    title: 'Terrenos',
    text: 'Encontrá terrenos para construir, invertir o desarrollar tu próximo proyecto.',
  },
  {
    img: '/images/stock/asesoria.jpg',
    title: 'Asesoría Inmobiliaria',
    text: 'Te acompañamos para tomar decisiones seguras y encontrar la mejor opción según tus necesidades.',
  },
]

// Fotos destacadas: la foto principal de propiedades disponibles, obtenidas de la API
// (mismo almacenamiento compartido que usa el resto del sitio y el panel). Se alternan
// casas y terrenos. Si no hay propiedades con fotos o la API no responde, se muestran
// ilustraciones del sitio (no son fotos de propiedades).
const MAX_DESTACADAS = 4
export const FOTOS_RESPALDO = [
  '/images/stock/compra-venta.jpg',
  '/images/stock/terreno.jpg',
  '/images/stock/alquiler.jpg',
  '/images/stock/asesoria.jpg',
]

/** Primera foto (la principal) de hasta 4 propiedades, alternando casas y terrenos. */
export function elegirFotosDestacadas(propiedades: PropiedadFeature[]): string[] {
  const conFoto = propiedades.filter((p) => (p.properties.fotos?.length ?? 0) > 0)
  const casas = conFoto.filter((p) => p.properties.categoria !== 'Terreno')
  const terrenos = conFoto.filter((p) => p.properties.categoria === 'Terreno')
  const elegidas: PropiedadFeature[] = []
  for (let i = 0; elegidas.length < MAX_DESTACADAS && (i < casas.length || i < terrenos.length); i++) {
    if (i < casas.length) elegidas.push(casas[i])
    if (i < terrenos.length && elegidas.length < MAX_DESTACADAS) elegidas.push(terrenos[i])
  }
  return elegidas.map((p) => p.properties.fotos![0])
}

function useFotosDestacadas(): string[] {
  const [fotos, setFotos] = useState<string[]>(FOTOS_RESPALDO)
  useEffect(() => {
    let vigente = true
    apiGet<GeoFeatureCollection<PropiedadFeature>>('/api/propiedades')
      .then((fc) => {
        const elegidas = elegirFotosDestacadas(fc.features)
        if (vigente && elegidas.length > 0) setFotos(elegidas)
      })
      .catch(() => {
        // sin conexión con el backend: se mantienen las ilustraciones de respaldo
      })
    return () => {
      vigente = false
    }
  }, [])
  return fotos
}

export default function Servicios() {
  const navigate = useNavigate()
  const fotosDestacadas = useFotosDestacadas()

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
            {/* key: al llegar las fotos de la API, el carrusel vuelve a la primera */}
            <ImageCarousel key={fotosDestacadas.join('|')} images={fotosDestacadas} />
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

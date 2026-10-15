import { useEffect, useState } from 'react'
import { ArrowRight } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { ImageCarousel } from '../busqueda/SearchFilter'
import { apiGet } from '../../services/api'
import type { GeoFeatureCollection, PropiedadFeature } from '../PropertyMap/types'
import { destacar } from '../comun/destacar'
import { useContenidoSitio } from '../../services/contenidoSitio'
import styles from './Servicios.module.css'

/** Fotos de fondo de las tarjetas de servicios, asignadas por posición. */
const IMAGENES_SERVICIOS = [
  '/images/stock/compra-venta.jpg',
  '/images/stock/alquiler.jpg',
  '/images/stock/terreno.jpg',
  '/images/stock/asesoria.jpg',
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
  const servicios = useContenidoSitio().contenido.servicios
  const navigate = useNavigate()
  const fotosDestacadas = useFotosDestacadas()

  return (
    <>
      {/* 1. Intro */}
      <section className={styles.intro}>
        <span className={styles.eyebrow}>Servicios</span>
        <h1 className={styles.title}>{destacar(servicios.titulo, styles.titleAccent)}</h1>
        {servicios.subtitulo && <p className={styles.introText}>{servicios.subtitulo}</p>}
      </section>

      {/* 2. Servicios (de 1 a 8, desde el panel); las fotos de fondo se asignan por posición */}
      <section className={styles.servicios}>
        <div className={styles.serviciosGrid}>
          {servicios.servicios.map((s, i) => (
            <div
              key={`${i}-${s.titulo}`}
              className={styles.servicioCard}
              style={{ backgroundImage: `url(${IMAGENES_SERVICIOS[i % IMAGENES_SERVICIOS.length]})` }}
            >
              <h3 className={styles.servicioTitle}>{s.titulo}</h3>
              <p className={styles.servicioText}>{s.texto}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 3. Terrenos y propiedades + fotos deslizables */}
      <section className={styles.destacado}>
        <div className={styles.destacadoGrid}>
          <div>
            <span className={styles.eyebrowDark}>Lo que tenemos para vos</span>
            {servicios.bloqueTitulo && <h2 className={styles.h2}>{destacar(servicios.bloqueTitulo, styles.h2Accent)}</h2>}
            {servicios.bloqueTexto && <p className={styles.body}>{servicios.bloqueTexto}</p>}
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

import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, Bath, BedDouble, Ruler } from 'lucide-react'
import { iconoCategoria, fotosPara } from '../busqueda/SearchFilter'
import { useGeoData } from '../PropertyMap/useGeoData'
import type { PropiedadFeature } from '../PropertyMap/types'
import styles from './OffersSection.module.css'

// Toma propiedades y terrenos reales (los mismos de /buscar), elegidos al
// azar en cada carga, y al hacer clic te lleva directo al detalle de esa
// propiedad dentro de la búsqueda (ubicación exacta + especificaciones).
function useOfertasAleatorias(propiedades: PropiedadFeature[], cantidad = 6) {
  return useMemo(() => {
    if (propiedades.length === 0) return []
    const copia = [...propiedades]
    for (let i = copia.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[copia[i], copia[j]] = [copia[j], copia[i]]
    }
    return copia.slice(0, Math.min(cantidad, copia.length))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [propiedades.length])
}

export default function OffersSection() {
  const navigate = useNavigate()
  const { propiedades, loading } = useGeoData()
  const ofertas = useOfertasAleatorias(propiedades, 6)

  return (
    <section className={styles.section}>
      <div className={styles.grid}>
        {/* Ofertas */}
        <div className={styles.ofertasPanel}>
          <span className={styles.eyebrow}>Ofertas destacadas</span>
          <h2 className={styles.title}>
            Propiedades y terrenos <em className={styles.titleAccent}>disponibles ahora</em>
          </h2>

          {loading ? (
            <p className={styles.loading}>Cargando ofertas…</p>
          ) : (
            <div className={styles.cardsGrid}>
              {ofertas.map((p) => (
                <OfferCard
                  key={p.properties.id}
                  propiedad={p}
                  onClick={() => navigate(`/buscar?propiedad=${p.properties.id}`)}
                />
              ))}
            </div>
          )}
        </div>

        {/* Sobre nosotros */}
        <div className={styles.nosotrosPanel}>
          <span className={styles.eyebrowLight}>Sobre nosotros</span>
          <h3 className={styles.subtitle}>
            Por qué elegirnos <em className={styles.subtitleAccent}>como inmobiliaria.</em>
          </h3>
          <p className={styles.nosotrosText}>
            Conocemos cada barrio de Alto Paraná casa por casa. Acompañamos todo el proceso —de
            la primera visita a la entrega de llaves— con información real, transparente y
            actualizada.
          </p>

          <img
            src="/images/stock/inicio_sobre_nosotros.jpg"
            alt="Entrega de llaves"
            className={styles.nosotrosImg}
          />

          <button className={styles.nosotrosBtn} onClick={() => navigate('/nosotros')}>
            Conocé más sobre nosotros <ArrowRight size={16} aria-hidden="true" />
          </button>
        </div>
      </div>
    </section>
  )
}

function OfferCard({ propiedad, onClick }: { propiedad: PropiedadFeature; onClick: () => void }) {
  const props = propiedad.properties
  const fotos = fotosPara(props)
  const foto = fotos[0]

  return (
    <div
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && onClick()}
      className={styles.card}
    >
      <div className={styles.cardImg}>
        {foto ? (
          <img src={foto} alt={props.titulo} className={styles.cardImgPhoto} />
        ) : (
          <span className={styles.cardImgIcon}>{iconoCategoria(props.categoria)}</span>
        )}
        <span className={styles.cardBadge}>{props.categoria}</span>
        <span className={styles.cardTag}>{props.tipoOperacion.toUpperCase()}</span>
      </div>
      <div className={styles.cardBody}>
        <div className={styles.cardPrice}>
          {props.moneda} {props.precio.toLocaleString()}
          {props.tipoOperacion === 'Alquiler' ? ' / mes' : ''}
        </div>
        <div className={styles.cardTitle}>{props.titulo}</div>
        <div className={styles.cardSpecs}>
          {props.categoria !== 'Terreno' && (
            <>
              <span className={styles.cardSpec}><BedDouble size={15} aria-hidden="true" /> {props.dormitorios}</span>
              <span className={styles.cardSpec}><Bath size={15} aria-hidden="true" /> {props.banos}</span>
            </>
          )}
          <span className={styles.cardSpec}><Ruler size={15} aria-hidden="true" /> {props.m2} m²</span>
        </div>
      </div>
    </div>
  )
}

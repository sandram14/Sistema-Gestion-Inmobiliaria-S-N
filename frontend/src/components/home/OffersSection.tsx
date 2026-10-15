import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bath, BedDouble, Ruler } from 'lucide-react'
import { iconoCategoria, fotosPara } from '../busqueda/SearchFilter'
import { useGeoData } from '../PropertyMap/useGeoData'
import type { PropiedadFeature } from '../PropertyMap/types'
import { elegirOfertas, useContenidoSitio } from '../../services/contenidoSitio'
import styles from './OffersSection.module.css'

export default function OffersSection() {
  const navigate = useNavigate()
  const { propiedades, loading } = useGeoData()
  const { contenido } = useContenidoSitio()
  const seleccion = contenido.ofertas.join(',')
  // Las elegidas en el panel, en su orden; sin selección, 6 al azar (se eligen una sola vez por carga)
  const ofertas = useMemo(
    () => elegirOfertas(propiedades, contenido.ofertas, 6),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [propiedades.length, seleccion],
  )

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
              {ofertas.map((p: PropiedadFeature) => (
                <OfferCard
                  key={p.properties.id}
                  propiedad={p}
                  onClick={() => navigate(`/buscar?propiedad=${p.properties.id}`)}
                />
              ))}
            </div>
          )}
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

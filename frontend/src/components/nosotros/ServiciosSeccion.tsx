import { destacar } from '../comun/destacar'
import { useContenidoSitio } from '../../services/contenidoSitio'
import styles from './ServiciosSeccion.module.css'

/** Fotos de fondo de las tarjetas de servicios, asignadas por posición. */
const IMAGENES_SERVICIOS = [
  '/images/stock/compra-venta.jpg',
  '/images/stock/alquiler.jpg',
  '/images/stock/terreno.jpg',
  '/images/stock/asesoria.jpg',
]

/**
 * Servicios dentro de la página Nosotros (antes era una página aparte; /servicios redirige aquí).
 * Título, subtítulo y tarjetas (de 1 a 8) se editan desde el panel, pestaña Nosotros.
 */
export default function ServiciosSeccion() {
  const servicios = useContenidoSitio().contenido.servicios

  return (
    <section id="servicios" className={styles.servicios}>
      <div className={styles.contenido}>
        {servicios.titulo && <h2 className={styles.h2}>{destacar(servicios.titulo, styles.h2Accent)}</h2>}
        {servicios.subtitulo && <p className={styles.body}>{servicios.subtitulo}</p>}
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
      </div>
    </section>
  )
}

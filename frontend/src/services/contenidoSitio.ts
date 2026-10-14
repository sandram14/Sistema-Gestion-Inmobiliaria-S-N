/**
 * Contenido del sitio editable desde el panel (Gestión del sitio web).
 * Contrato del backend: docs/integracion-gestion-sitio.md
 *
 * Uso en un componente:
 *   const { contenido } = useContenidoSitio()
 *   <h1>{contenido.inicio.titulo}</h1>
 *
 * Si el backend no responde, se usan los textos que el sitio mostraba antes (CONTENIDO_RESPALDO),
 * así ninguna sección queda vacía.
 */
import { useEffect, useState } from 'react'
import { apiGet } from './api'

export interface ItemTexto { titulo: string; texto: string }

export interface ContenidoSitio {
  inicio: {
    /** null: usar la imagen de fondo del diseño */
    imagenPortada: string | null
    etiqueta: string
    titulo: string
    subtitulo: string
    nosotrosTitulo: string
    nosotrosTexto: string
    /** null: usar la imagen del diseño */
    nosotrosImagen: string | null
  }
  servicios: { titulo: string; subtitulo: string; servicios: ItemTexto[]; bloqueTitulo: string; bloqueTexto: string }
  nosotros: {
    introduccion: string; historiaTitulo: string; historiaTexto: string; imagen: string | null
    cifras: { valor: string; etiqueta: string }[]; valores: ItemTexto[]; razones: ItemTexto[]
  }
  contacto: {
    nombreEmpresa: string; descripcion: string; direccion: string; ciudad: string
    email: string; telefono: string
    /** Solo números, formato internacional: https://wa.me/{whatsapp} */
    whatsapp: string
    horario: string
  }
  /** true mientras el email, el teléfono o el WhatsApp sean los de prueba de la instalación */
  contactoProvisional: boolean
  /** Códigos de las propiedades destacadas DISPONIBLES, en orden. Vacío: mostrar 6 al azar (comportamiento anterior) */
  ofertas: string[]
}

/** Los textos que el sitio tenía escritos en su código (idénticos al contenido inicial de la migración 006). */
export const CONTENIDO_RESPALDO: ContenidoSitio = {
  inicio: {
    imagenPortada: null,
    etiqueta: 'Más de 12.000 propiedades disponibles',
    titulo: 'Encuentra el lugar donde comenzará tu próxima historia.',
    subtitulo: 'S&N Inmobiliaria conecta compradores e inversores con las mejores propiedades, con tecnología que simplifica cada decisión.',
    nosotrosTitulo: 'Por qué elegirnos como inmobiliaria.',
    nosotrosTexto: 'Conocemos cada barrio de Alto Paraná casa por casa. Acompañamos todo el proceso —de la primera visita a la entrega de llaves— con información real, transparente y actualizada.',
    nosotrosImagen: null,
  },
  servicios: {
    titulo: 'Nuestros Servicios',
    subtitulo: 'Una solución para cada necesidad.',
    servicios: [
      { titulo: 'Compra y Venta', texto: 'Te asesoramos durante todo el proceso de adquisición o venta de inmuebles.' },
      { titulo: 'Alquileres', texto: 'Encontrá la propiedad ideal para alquilar de forma rápida y confiable.' },
      { titulo: 'Terrenos', texto: 'Encontrá terrenos para construir, invertir o desarrollar tu próximo proyecto.' },
      { titulo: 'Asesoría Inmobiliaria', texto: 'Te acompañamos para tomar decisiones seguras y encontrar la mejor opción según tus necesidades.' },
    ],
    bloqueTitulo: 'Encontrá el terreno o la propiedad ideal para tu proyecto.',
    bloqueTexto: 'Contamos con terrenos y propiedades ubicados en diferentes zonas de Alto Paraná, ideales para vivienda, inversión o desarrollo comercial.',
  },
  nosotros: {
    introduccion: 'Somos una plataforma enfocada en conectar a las personas con las mejores oportunidades inmobiliarias, brindando un servicio transparente, moderno y adaptado a tus necesidades.',
    historiaTitulo: 'Un equipo que conoce Alto Paraná de cerca.',
    historiaTexto: 'Nacimos en Ciudad del Este con un objetivo simple: que comprar, vender o alquilar una propiedad sea un proceso claro, sin sorpresas. Hoy acompañamos a familias e inversores en toda la región, barrio por barrio.',
    imagen: null,
    cifras: [{ valor: '+150', etiqueta: 'Propiedades' }, { valor: '+12', etiqueta: 'Barrios cubiertos' }, { valor: '+6', etiqueta: 'Años de experiencia' }],
    valores: [
      { titulo: 'Confianza', texto: 'Construimos cada relación sobre información real y compromisos que cumplimos.' },
      { titulo: 'Transparencia', texto: 'Sin letra chica: precios, estados y condiciones claras desde el primer contacto.' },
      { titulo: 'Compromiso', texto: 'Acompañamos todo el proceso, de la primera visita a la entrega de llaves.' },
      { titulo: 'Innovación', texto: 'Herramientas como el mapa por barrios para que encuentres más rápido.' },
    ],
    razones: [
      { titulo: 'Conocemos cada barrio', texto: 'Cubrimos Alto Paraná zona por zona, con información actualizada de cada barrio.' },
      { titulo: 'Búsqueda inteligente', texto: 'Filtrá por mapa, precio y tipo de propiedad hasta encontrar justo lo que buscás.' },
      { titulo: 'Acompañamiento real', texto: 'Un agente especializado te guía en cada paso, sin apuros ni letra chica.' },
    ],
  },
  contacto: {
    nombreEmpresa: 'S&N Inmobiliaria',
    descripcion: 'La plataforma inmobiliaria más avanzada de Alto Paraná para comprar, alquilar e invertir con confianza.',
    direccion: '', ciudad: 'Ciudad del Este', email: '', telefono: '', whatsapp: '', horario: '',
  },
  contactoProvisional: true,
  ofertas: [],
}

let cache: Promise<ContenidoSitio> | null = null

/** Lee GET /api/sitio una vez por carga de página. Si falla, devuelve CONTENIDO_RESPALDO. */
export function obtenerContenidoSitio(): Promise<ContenidoSitio> {
  if (!cache) {
    cache = apiGet<Partial<ContenidoSitio>>('/api/sitio')
      .then((r) => ({
        inicio: { ...CONTENIDO_RESPALDO.inicio, ...r.inicio },
        servicios: { ...CONTENIDO_RESPALDO.servicios, ...r.servicios },
        nosotros: { ...CONTENIDO_RESPALDO.nosotros, ...r.nosotros },
        contacto: { ...CONTENIDO_RESPALDO.contacto, ...r.contacto },
        contactoProvisional: r.contactoProvisional ?? true,
        ofertas: r.ofertas ?? [],
      }))
      .catch(() => {
        cache = null // se reintenta en la próxima llamada
        return CONTENIDO_RESPALDO
      })
  }
  return cache
}

/** Hook para componentes: devuelve el respaldo de inmediato y el contenido real apenas llega. */
export function useContenidoSitio(): { contenido: ContenidoSitio; cargando: boolean } {
  const [contenido, setContenido] = useState<ContenidoSitio>(CONTENIDO_RESPALDO)
  const [cargando, setCargando] = useState(true)
  useEffect(() => {
    let activo = true
    obtenerContenidoSitio().then((c) => { if (activo) { setContenido(c); setCargando(false) } })
    return () => { activo = false }
  }, [])
  return { contenido, cargando }
}

/**
 * Ofertas de la portada: si hay selección, las elegidas en su orden; si no, `cantidad` al azar (comportamiento anterior).
 * `todas` es la lista de propiedades que el componente ya carga de /api/propiedades.
 */
export function elegirOfertas<T extends { properties: { id: string } }>(todas: T[], seleccion: string[], cantidad = 6): T[] {
  if (seleccion.length > 0) {
    const porCodigo = new Map(todas.map((p) => [p.properties.id, p]))
    return seleccion.map((c) => porCodigo.get(c)).filter((p): p is T => p !== undefined)
  }
  const copia = [...todas]
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1)); [copia[i], copia[j]] = [copia[j], copia[i]]
  }
  return copia.slice(0, Math.min(cantidad, copia.length))
}

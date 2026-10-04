import { useEffect, useRef, useState } from 'react'
import styles from './AssistantWidget.module.css'
import { ApiError, apiPost } from '../../services/api'

// Número del agente que recibe las consultas generadas por el asistente
// (mismo formato/agente que ya se usa en la página de Contacto).
// PENDIENTE: reemplazar por el número real de la inmobiliaria (formato internacional sin "+").
const AGENTE_WHATSAPP = '595981123456'

/** Máximo de caracteres por pregunta (el backend rechaza más de 500). */
const MAX_PREGUNTA = 500

// ─── Abrir el asistente desde otra parte de la web ──────────────────────────
// El detalle de una propiedad (SearchFilter.tsx) emite este evento con su código, y el
// asistente se abre listo para responder sobre esa propiedad. Así no hace falta modificar
// App.tsx ni compartir estado entre componentes.
const EVENTO_CONSULTAR = 'sn:consultar-propiedad'

interface PropiedadEnConsulta {
  codigo: string
  titulo: string
}

export function abrirAsistenteConPropiedad(propiedad: PropiedadEnConsulta) {
  window.dispatchEvent(new CustomEvent<PropiedadEnConsulta>(EVENTO_CONSULTAR, { detail: propiedad }))
}

/** Respuesta de POST /api/asistente/consultas */
interface RespuestaAsistente {
  respuesta: string
  sugerirWhatsApp: boolean
  propiedadesRelacionadas: string[]
}

const CIUDADES = ['Ciudad del Este', 'Hernandarias', 'Minga Guazú', 'Presidente Franco', 'No estoy seguro/a']

const PRESUPUESTOS = ['Hasta USD 40.000', 'Hasta USD 70.000', 'Hasta USD 100.000', 'Hasta USD 160.000', 'Más de USD 160.000', 'Prefiero no decir']

type Step = 'inicio' | 'tipoVivienda' | 'zona' | 'presupuesto' | 'contacto' | 'resumen'

// whatsapp: mensaje para el enlace de WhatsApp cuando el asistente sugiere hablar con un asesor
type ChatMessage = { from: 'bot' | 'user'; text: string; whatsapp?: string }

interface Respuestas {
  operacion?: string
  tipoOperacion?: 'Venta' | 'Alquiler'
  categoria?: string
  zona?: string
  presupuesto?: string
  nombre?: string
  telefono?: string
}

const SALUDO_INICIAL: ChatMessage = {
  from: 'bot',
  text: '¡Hola! 👋 Soy el asistente virtual de S&N Inmobiliaria. Contame qué estás buscando y te ayudo a conectarte con un agente.',
}

export default function AssistantWidget() {
  const [open, setOpen] = useState(false)
  const [minimized, setMinimized] = useState(false)
  const [step, setStep] = useState<Step>('inicio')
  const [messages, setMessages] = useState<ChatMessage[]>([SALUDO_INICIAL])
  const [respuestas, setRespuestas] = useState<Respuestas>({})
  const [nombreInput, setNombreInput] = useState('')
  const [telefonoInput, setTelefonoInput] = useState('')
  const [pregunta, setPregunta] = useState('')
  const [esperando, setEsperando] = useState(false)
  const [propiedadEnConsulta, setPropiedadEnConsulta] = useState<PropiedadEnConsulta | null>(null)

  // "Consultar sobre esta propiedad" desde el detalle
  useEffect(() => {
    function alConsultar(e: Event) {
      const propiedad = (e as CustomEvent<PropiedadEnConsulta>).detail
      setPropiedadEnConsulta(propiedad)
      setOpen(true)
      setMinimized(false)
      decir(`Escribí tu consulta sobre "${propiedad.titulo}" (${propiedad.codigo}) y te respondo con los datos que tenemos.`)
    }
    window.addEventListener(EVENTO_CONSULTAR, alConsultar)
    return () => window.removeEventListener(EVENTO_CONSULTAR, alConsultar)
  }, [])

  const bodyRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight
    }
  }, [messages, step])

  function decir(text: string) {
    setMessages((m) => [...m, { from: 'bot', text }])
  }

  function responder(text: string) {
    setMessages((m) => [...m, { from: 'user', text }])
  }

  function elegirOperacion(op: 'Comprar' | 'Alquilar' | 'Terreno') {
    responder(op === 'Terreno' ? 'Quiero comprar un terreno' : `Quiero ${op.toLowerCase()} una propiedad`)

    if (op === 'Terreno') {
      setRespuestas((r) => ({ ...r, operacion: op, tipoOperacion: 'Venta', categoria: 'Terreno' }))
      setStep('zona')
      decir('¡Buenísimo! ¿En qué ciudad te gustaría buscar el terreno?')
    } else {
      setRespuestas((r) => ({
        ...r,
        operacion: op,
        tipoOperacion: op === 'Comprar' ? 'Venta' : 'Alquiler',
        categoria: 'Vivienda',
      }))
      setStep('tipoVivienda')
      decir('¿Casa, departamento, o te da igual cuál de las dos?')
    }
  }

  function elegirTipoVivienda(tipo: string) {
    responder(tipo)
    setRespuestas((r) => ({ ...r, categoria: `Vivienda (${tipo})` }))
    setStep('zona')
    decir('¿En qué ciudad te gustaría buscar?')
  }

  function elegirZona(ciudad: string) {
    responder(ciudad)
    setRespuestas((r) => ({ ...r, zona: ciudad }))
    setStep('presupuesto')
    decir('¿Cuál es tu presupuesto aproximado?')
  }

  function elegirPresupuesto(rango: string) {
    responder(rango)
    setRespuestas((r) => ({ ...r, presupuesto: rango }))
    setStep('contacto')
    decir('Perfecto. Para que un agente se contacte con vos, dejame tu nombre y un teléfono.')
  }

  function enviarContacto() {
    if (!nombreInput.trim() || !telefonoInput.trim()) return
    responder(`${nombreInput} — ${telefonoInput}`)
    setRespuestas((r) => ({ ...r, nombre: nombreInput.trim(), telefono: telefonoInput.trim() }))
    setStep('resumen')
    decir('¡Listo! Este es el resumen que le vamos a compartir a nuestro equipo. Tocá "Enviar por WhatsApp" para que un agente te contacte.')
  }

  function mensajeWhatsApp(r: Respuestas): string {
    const lineas = [
      `Hola, soy ${r.nombre}. Quisiera hacer una consulta desde la web de S&N Inmobiliaria:`,
      `• Operación: ${r.operacion === 'Terreno' ? 'Comprar terreno' : `${r.operacion} ${r.categoria ?? ''}`}`,
      `• Zona: ${r.zona}`,
      `• Presupuesto: ${r.presupuesto}`,
      `• Teléfono de contacto: ${r.telefono}`,
    ]
    return encodeURIComponent(lineas.join('\n'))
  }

  function reiniciar() {
    setMessages([SALUDO_INICIAL])
    setRespuestas({})
    setNombreInput('')
    setTelefonoInput('')
    setStep('inicio')
    setPregunta('')
    setPropiedadEnConsulta(null)
  }

  // ─── Pregunta libre al asistente de IA (Gemini, a través del backend) ─────
  async function enviarPregunta() {
    const texto = pregunta.trim()
    if (!texto || esperando) return
    responder(texto)
    setPregunta('')
    setEsperando(true)
    try {
      const r = await apiPost<RespuestaAsistente>('/api/asistente/consultas', {
        pregunta: texto,
        codigoPropiedad: propiedadEnConsulta?.codigo ?? null,
      })
      const contexto = propiedadEnConsulta ? ` sobre la propiedad ${propiedadEnConsulta.codigo}` : ''
      setMessages((m) => [
        ...m,
        {
          from: 'bot',
          text: r.respuesta,
          whatsapp: r.sugerirWhatsApp
            ? encodeURIComponent(`Hola, quisiera hacer una consulta${contexto} desde la web de S&N Inmobiliaria: ${texto}`)
            : undefined,
        },
      ])
    } catch (e) {
      // 429 (muchas consultas), 503 (asistente no disponible) y otros: el backend envía un mensaje claro
      const mensaje =
        e instanceof ApiError ? e.mensajeParaVisitante() : 'No pude responder en este momento. Intentá de nuevo en unos minutos.'
      setMessages((m) => [
        ...m,
        {
          from: 'bot',
          text: mensaje,
          whatsapp: encodeURIComponent('Hola, quisiera hacer una consulta desde la web de S&N Inmobiliaria.'),
        },
      ])
    } finally {
      setEsperando(false)
    }
  }

  return (
    <>
      {!open && (
        <button className={styles.fab} onClick={() => setOpen(true)} aria-label="Abrir asistente">
          <span className={styles.fabIcon}>💬</span>
          <span className={styles.fabLabel}>Asistente</span>
        </button>
      )}

      {open && (
        <div className={`${styles.window} ${minimized ? styles.windowMinimized : ''}`}>
          <div
            className={styles.header}
            onClick={() => minimized && setMinimized(false)}
            role={minimized ? 'button' : undefined}
          >
            <div className={styles.headerTitle}>
              <span className={styles.headerDot} />
              Asistente S&N
            </div>
            <div className={styles.headerActions}>
              <button
                className={styles.headerBtn}
                onClick={(e) => {
                  e.stopPropagation()
                  setMinimized((v) => !v)
                }}
                aria-label={minimized ? 'Restaurar' : 'Minimizar'}
              >
                {minimized ? '▢' : '–'}
              </button>
              <button
                className={styles.headerBtn}
                onClick={(e) => {
                  e.stopPropagation()
                  setOpen(false)
                  setMinimized(false)
                }}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>
          </div>

          {!minimized && (
            <>
              <div className={styles.body} ref={bodyRef}>
                {messages.map((m, i) => (
                  <div key={i}>
                    {/* pre-line: respeta los saltos de línea de las listas del asistente */}
                    <div className={m.from === 'bot' ? styles.bubbleBot : styles.bubbleUser} style={{ whiteSpace: 'pre-line' }}>
                      {m.text}
                    </div>
                    {m.whatsapp && (
                      <a
                        className={styles.whatsappBtn}
                        href={`https://wa.me/${AGENTE_WHATSAPP}?text=${m.whatsapp}`}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Hablar con un asesor por WhatsApp →
                      </a>
                    )}
                  </div>
                ))}

                {esperando && <div className={styles.bubbleBot}>Escribiendo…</div>}

                {step === 'resumen' && (
                  <div className={styles.summaryCard}>
                    <div className={styles.summaryRow}>
                      <span>Operación</span>
                      <strong>{respuestas.operacion === 'Terreno' ? 'Comprar terreno' : `${respuestas.operacion} ${respuestas.categoria}`}</strong>
                    </div>
                    <div className={styles.summaryRow}>
                      <span>Zona</span>
                      <strong>{respuestas.zona}</strong>
                    </div>
                    <div className={styles.summaryRow}>
                      <span>Presupuesto</span>
                      <strong>{respuestas.presupuesto}</strong>
                    </div>
                    <div className={styles.summaryRow}>
                      <span>Contacto</span>
                      <strong>
                        {respuestas.nombre} · {respuestas.telefono}
                      </strong>
                    </div>
                  </div>
                )}
              </div>

              <div className={styles.actions}>
                {step === 'inicio' && (
                  <>
                    <button className={styles.chip} onClick={() => elegirOperacion('Comprar')}>
                      Comprar una propiedad
                    </button>
                    <button className={styles.chip} onClick={() => elegirOperacion('Alquilar')}>
                      Alquilar una propiedad
                    </button>
                    <button className={styles.chip} onClick={() => elegirOperacion('Terreno')}>
                      Comprar un terreno
                    </button>
                  </>
                )}

                {step === 'tipoVivienda' && (
                  <>
                    <button className={styles.chip} onClick={() => elegirTipoVivienda('Casa')}>
                      Casa
                    </button>
                    <button className={styles.chip} onClick={() => elegirTipoVivienda('Departamento')}>
                      Departamento
                    </button>
                    <button className={styles.chip} onClick={() => elegirTipoVivienda('Cualquiera')}>
                      Me da igual
                    </button>
                  </>
                )}

                {step === 'zona' &&
                  CIUDADES.map((c) => (
                    <button key={c} className={styles.chip} onClick={() => elegirZona(c)}>
                      {c}
                    </button>
                  ))}

                {step === 'presupuesto' &&
                  PRESUPUESTOS.map((p) => (
                    <button key={p} className={styles.chip} onClick={() => elegirPresupuesto(p)}>
                      {p}
                    </button>
                  ))}

                {step === 'contacto' && (
                  <form
                    className={styles.form}
                    onSubmit={(e) => {
                      e.preventDefault()
                      enviarContacto()
                    }}
                  >
                    <input
                      className={styles.input}
                      placeholder="Tu nombre"
                      value={nombreInput}
                      onChange={(e) => setNombreInput(e.target.value)}
                    />
                    <input
                      className={styles.input}
                      placeholder="Tu teléfono"
                      value={telefonoInput}
                      onChange={(e) => setTelefonoInput(e.target.value)}
                    />
                    <button type="submit" className={styles.sendBtn}>
                      Continuar
                    </button>
                  </form>
                )}

                {step !== 'contacto' && (
                  <form
                    className={styles.form}
                    onSubmit={(e) => {
                      e.preventDefault()
                      void enviarPregunta()
                    }}
                  >
                    <input
                      className={styles.input}
                      placeholder={
                        propiedadEnConsulta ? `Preguntá sobre ${propiedadEnConsulta.codigo}…` : 'Escribí tu pregunta…'
                      }
                      value={pregunta}
                      maxLength={MAX_PREGUNTA}
                      disabled={esperando}
                      onChange={(e) => setPregunta(e.target.value)}
                      aria-label="Pregunta para el asistente"
                    />
                    <button type="submit" className={styles.sendBtn} disabled={esperando || !pregunta.trim()}>
                      Enviar
                    </button>
                    {propiedadEnConsulta && (
                      <button type="button" className={styles.restartBtn} onClick={() => setPropiedadEnConsulta(null)}>
                        Preguntar en general
                      </button>
                    )}
                  </form>
                )}

                {step === 'resumen' && (
                  <>
                    <a
                      className={styles.whatsappBtn}
                      href={`https://wa.me/${AGENTE_WHATSAPP}?text=${mensajeWhatsApp(respuestas)}`}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Enviar por WhatsApp →
                    </a>
                    <button className={styles.restartBtn} onClick={reiniciar}>
                      Empezar de nuevo
                    </button>
                  </>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </>
  )
}

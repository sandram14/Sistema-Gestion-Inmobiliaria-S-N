import { useEffect, useMemo, useState, type CSSProperties, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Building2, Globe, MessagesSquare, Plus, UserPlus, UserRoundCheck } from 'lucide-react'
import { api } from '../api/cliente'
import type { PaginaClientes, Permiso } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import ui from '../ui.module.css'

/** Saludo según la hora del día (sin suponer el género de nadie: no se usa "Bienvenido/a"). */
export function saludoDeLaHora(hora: number): string {
  if (hora >= 5 && hora < 12) return 'Buenos días'
  if (hora >= 12 && hora < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

export const FRASES = [
  '¿Por dónde empezamos?',
  'Este es su panel de trabajo.',
  '¿Qué vamos a hacer hoy?',
  'Todo listo para seguir.',
]

interface Ficha { clave: string; permiso: Permiso; destino: string; icono: ReactNode; titulo: string; texto: string }

/**
 * Accesos de Inicio, en orden de prioridad: se muestran los primeros 4 que el rol puede usar
 * (administrador: clientes, nueva propiedad, nuevo usuario y sitio web; gerente: clientes, nueva propiedad,
 * sitio web y consultas; vendedor: clientes, nueva propiedad, consultas y listado).
 */
const ACCESOS: Ficha[] = [
  { clave: 'propiedad', permiso: 'PROPIEDADES_EDITAR', destino: '/propiedades/nueva', icono: <Plus size={22} aria-hidden="true" />,
    titulo: 'Nueva propiedad', texto: 'Cargar una casa o un terreno.' },
  { clave: 'usuario', permiso: 'USUARIOS_GESTIONAR', destino: '/usuarios/nuevo', icono: <UserPlus size={22} aria-hidden="true" />,
    titulo: 'Nuevo usuario', texto: 'Dar acceso al panel a una persona del equipo.' },
  { clave: 'sitio', permiso: 'CONTENIDO_EDITAR', destino: '/sitio', icono: <Globe size={22} aria-hidden="true" />,
    titulo: 'Gestión del sitio web', texto: 'Portada, ofertas, Nosotros y Contacto.' },
  { clave: 'consultas', permiso: 'CONSULTAS_VER', destino: '/consultas', icono: <MessagesSquare size={22} aria-hidden="true" />,
    titulo: 'Consultas IA', texto: 'Lo que preguntan los visitantes en el chat.' },
  { clave: 'listado', permiso: 'PROPIEDADES_VER', destino: '/propiedades', icono: <Building2 size={22} aria-hidden="true" />,
    titulo: 'Listado de propiedades', texto: 'Ver y editar las propiedades cargadas.' },
]

/** Pantalla de Inicio del panel: saludo, una frase y de 2 a 4 accesos según el rol. */
export default function Inicio() {
  const { usuario, puede } = useSesion()
  const [pendientes, setPendientes] = useState<number | null>(null)
  const [errorPendientes, setErrorPendientes] = useState(false)
  const verClientes = puede('CLIENTES_VER')
  // La frase se elige una vez por visita a Inicio
  const frase = useMemo(() => FRASES[Math.floor(Math.random() * FRASES.length)], [])

  useEffect(() => {
    if (!verClientes) return
    // El backend ya filtra por rol: al vendedor solo le cuenta los suyos
    api.get<PaginaClientes>('/api/admin/clientes-potenciales?pagina=1')
      .then((r) => setPendientes(r.conteos.pendientes))
      .catch(() => setErrorPendientes(true))
  }, [verClientes])

  const accesos = ACCESOS.filter((a) => puede(a.permiso)).slice(0, verClientes ? 3 : 4)
  const total = accesos.length + (verClientes ? 1 : 0)

  return (
    <div className={ui.inicio}>
      <div className={ui.inicioSaludo}>
        <h1>{saludoDeLaHora(new Date().getHours())}, {usuario?.nombre}</h1>
        <p>{frase}</p>
      </div>

      <div className={ui.fichasInicio} role="group" aria-label="Accesos rápidos"
        // Columnas según cuántas fichas hay: con 4, en pantallas medianas van 2 por fila (nunca 3 + 1)
        style={{ '--columnas': total, '--columnas-medio': total === 4 ? 2 : total } as CSSProperties}>
        {verClientes && (
          <Link to="/consultas/clientes?estado=PENDIENTE"
            className={`${ui.fichaInicio} ${pendientes ? ui.fichaInicioAviso : ''}`}>
            <span className={ui.fichaInicioIcono}><UserRoundCheck size={22} aria-hidden="true" /></span>
            {errorPendientes ? (
              <>
                <strong>Clientes potenciales</strong>
                <span>No se pudo consultar cuántos esperan contacto.</span>
              </>
            ) : pendientes == null ? (
              <>
                <strong>Clientes potenciales</strong>
                <span>Consultando…</span>
              </>
            ) : pendientes > 0 ? (
              <>
                <span className={ui.fichaInicioNumero}>{pendientes}</span>
                <strong>{pendientes === 1 ? 'Cliente por contactar' : 'Clientes por contactar'}</strong>
                <span>{pendientes === 1 ? 'Dejó sus datos y espera contacto.' : 'Dejaron sus datos y esperan contacto.'}</span>
              </>
            ) : (
              <>
                <strong>Clientes potenciales</strong>
                <span>Al día: no hay clientes esperando contacto.</span>
              </>
            )}
          </Link>
        )}
        {accesos.map((a) => (
          <Link key={a.clave} to={a.destino} className={ui.fichaInicio}>
            <span className={ui.fichaInicioIcono}>{a.icono}</span>
            <strong>{a.titulo}</strong>
            <span>{a.texto}</span>
          </Link>
        ))}
      </div>
    </div>
  )
}

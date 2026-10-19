import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Monitor, Moon, Pencil, Sun } from 'lucide-react'
import { api } from '../api/cliente'
import type { UsuarioDto } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import { erroresDe } from '../componentes/errores'
import { nombreDelRol } from '../componentes/formato'
import { useTema, type Tema } from '../componentes/tema'
import ui from '../ui.module.css'

type CampoDatos = 'nombre' | 'apellido' | 'cedula' | 'telefono'

/** Cuenta propia en filas horizontales: cada dato se edita en su lugar. */
export default function MiCuenta() {
  const { recargar } = useSesion()
  const navegar = useNavigate()
  const [cuenta, setCuenta] = useState<UsuarioDto | null>(null)
  const [editando, setEditando] = useState<CampoDatos | 'email' | null>(null)
  const [valor, setValor] = useState('')
  const [passwordActual, setPasswordActual] = useState('')
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>((useLocation().state as { mensaje?: string } | null)?.mensaje ?? null)
  const [tema, setTema] = useTema()

  useEffect(() => { api.get<UsuarioDto>('/api/admin/cuenta').then(setCuenta).catch((e) => setGeneral(erroresDe(e).general)) }, [])

  function editar(campo: CampoDatos | 'email') {
    if (!cuenta) return
    setEditando(campo); setValor((cuenta[campo] as string | null) ?? ''); setPasswordActual(''); setCampos({}); setGeneral(null); setMensaje(null)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (!cuenta || !editando) return
    setCampos({}); setGeneral(null)
    try {
      const actualizada = editando === 'email'
        ? await api.put<UsuarioDto>('/api/admin/cuenta/email', { email: valor, passwordActual })
        : await api.put<UsuarioDto>('/api/admin/cuenta', {
          nombre: cuenta.nombre, apellido: cuenta.apellido, cedula: cuenta.cedula ?? '', telefono: cuenta.telefono ?? '', [editando]: valor,
        })
      setCuenta(actualizada); setEditando(null); await recargar()
      setMensaje(editando === 'email' ? 'Email actualizado. Úselo para iniciar sesión.' : 'Cambios guardados.')
    } catch (err) { const r = erroresDe(err); setCampos(r.campos); setGeneral(r.general) }
  }

  if (!cuenta) return general ? <div className={ui.avisoError}>{general}</div> : <p className={ui.cargando}>Cargando…</p>

  const ETIQUETAS: Record<CampoDatos | 'email', string> = { nombre: 'Nombre', apellido: 'Apellido', cedula: 'Cédula', telefono: 'Teléfono', email: 'Email' }
  const MAXIMO: Record<CampoDatos | 'email', number> = { nombre: 60, apellido: 60, cedula: 20, telefono: 30, email: 120 }

  /** Una fila: etiqueta, valor y botón de edición; al editar, el formulario aparece en la misma fila. */
  const fila = (campo: CampoDatos | 'email', vacio = 'No indicado') => (
    <div className={ui.filaCuenta} key={campo}>
      {editando === campo ? (
        <form onSubmit={guardar} className={ui.filaEdicion} noValidate>
          <label className={ui.campo}><span className={ui.etiqueta}>{ETIQUETAS[campo]}</span>
            <input className={ui.input} autoFocus type={campo === 'email' ? 'email' : 'text'} maxLength={MAXIMO[campo]} value={valor} onChange={(e) => setValor(e.target.value)} />
            {campos[campo] && <span className={ui.errorCampo}>{campos[campo]}</span>}</label>
          {campo === 'email' && (
            <label className={ui.campo}><span className={ui.etiqueta}>Contraseña actual (para confirmar)</span>
              <input className={ui.input} type="password" autoComplete="current-password" value={passwordActual} onChange={(e) => setPasswordActual(e.target.value)} />
              {campos.passwordActual && <span className={ui.errorCampo}>{campos.passwordActual}</span>}</label>
          )}
          <div className={ui.acciones}>
            <button type="button" className={`${ui.botonSecundario} ${ui.botonChico}`} onClick={() => setEditando(null)}>Cancelar</button>
            <button type="submit" className={`${ui.boton} ${ui.botonChico}`}>Guardar</button>
          </div>
        </form>
      ) : (
        <>
          <span className={ui.filaEtiqueta}>{ETIQUETAS[campo]}:</span>
          <span className={ui.filaValor}>{(cuenta[campo] as string | null) || <span className={ui.secundario}>{vacio}</span>}</span>
          <button type="button" className={ui.botonIcono} onClick={() => editar(campo)} aria-label={`Editar ${ETIQUETAS[campo].toLowerCase()}`} title="Editar">
            <Pencil size={16} aria-hidden="true" />
          </button>
        </>
      )}
    </div>
  )

  const opcionTema = (t: Tema, icono: ReactNode, texto: string) => (
    <button type="button" role="radio" aria-checked={tema === t} className={`${ui.opcionTema} ${tema === t ? ui.opcionTemaActiva : ''}`} onClick={() => setTema(t)}>
      {icono} {texto}
    </button>
  )

  return (
    <>
      <div className={ui.tituloPagina}><h1>Mi cuenta</h1></div>
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}

      <section className={ui.tarjeta}>
        <h2>Información de la cuenta</h2>
        <div className={ui.filaCuenta}>
          <span className={ui.filaEtiqueta}>Rol:</span>
          <span className={ui.filaValor}><span className={ui.estado}>{nombreDelRol(cuenta.rol)}</span></span>
          <span className={ui.secundario}>Lo asigna un administrador</span>
        </div>
        {fila('nombre')}
        {fila('apellido')}
        {fila('email')}
        <div className={ui.filaCuenta}>
          <span className={ui.filaEtiqueta}>Contraseña:</span>
          <span className={ui.filaValor}>••••••••</span>
          <button type="button" className={ui.botonIcono} onClick={() => navegar('/cuenta/contrasena')} aria-label="Cambiar contraseña" title="Cambiar contraseña">
            <Pencil size={16} aria-hidden="true" />
          </button>
        </div>
      </section>

      <section className={ui.tarjeta}>
        <h2>Datos personales</h2>
        {fila('cedula')}
        {fila('telefono')}
      </section>

      <section className={ui.tarjeta}>
        <h2>Preferencias</h2>
        <div className={ui.filaCuenta}>
          <span className={ui.filaEtiqueta}>Tema del sistema:</span>
          <div className={ui.selectorTema} role="radiogroup" aria-label="Tema del sistema">
            {opcionTema('claro', <Sun size={16} aria-hidden="true" />, 'Claro')}
            {opcionTema('oscuro', <Moon size={16} aria-hidden="true" />, 'Oscuro')}
            {opcionTema('sistema', <Monitor size={16} aria-hidden="true" />, 'Según el sistema')}
          </div>
        </div>
      </section>
    </>
  )
}

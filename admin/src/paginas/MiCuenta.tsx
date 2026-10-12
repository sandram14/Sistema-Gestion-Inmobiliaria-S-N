import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api/cliente'
import type { UsuarioDto } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import { erroresDe } from '../componentes/errores'
import { nombreDelRol } from '../componentes/formato'
import ui from '../ui.module.css'

/** Cuenta propia: datos personales, email (con la contraseña actual) y acceso al cambio de contraseña. */
export default function MiCuenta() {
  const { recargar } = useSesion()
  const [cuenta, setCuenta] = useState<UsuarioDto | null>(null)
  const [datos, setDatos] = useState({ nombre: '', apellido: '', cedula: '', telefono: '' })
  const [email, setEmail] = useState({ email: '', passwordActual: '' })
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [mensaje, setMensaje] = useState<string | null>((useLocation().state as { mensaje?: string } | null)?.mensaje ?? null)

  function cargar(u: UsuarioDto) {
    setCuenta(u)
    setDatos({ nombre: u.nombre, apellido: u.apellido, cedula: u.cedula ?? '', telefono: u.telefono ?? '' })
    setEmail({ email: u.email, passwordActual: '' })
  }

  useEffect(() => {
    api.get<UsuarioDto>('/api/admin/cuenta').then(cargar).catch((e) => setGeneral(erroresDe(e).general))
  }, [])

  async function enviar(e: FormEvent, accion: () => Promise<UsuarioDto>, texto: string) {
    e.preventDefault()
    setCampos({})
    setGeneral(null)
    setMensaje(null)
    try {
      cargar(await accion())
      await recargar()
      setMensaje(texto)
    } catch (err) {
      const r = erroresDe(err)
      setCampos(r.campos)
      setGeneral(r.general)
    }
  }

  const err = (c: string) => campos[c] && <span className={ui.errorCampo}>{campos[c]}</span>
  if (!cuenta) return general ? <div className={ui.avisoError}>{general}</div> : <p className={ui.cargando}>Cargando…</p>

  return (
    <>
      <div className={ui.tituloPagina}>
        <h1>Mi cuenta</h1>
        <Link to="/cuenta/contrasena" className={ui.botonSecundario}>Cambiar contraseña</Link>
      </div>
      {mensaje && <div className={ui.avisoExito}>{mensaje}</div>}
      {general && <div className={ui.avisoError} role="alert">{general}</div>}

      <form onSubmit={(e) => enviar(e, () => api.put<UsuarioDto>('/api/admin/cuenta', datos), 'Datos guardados.')} noValidate>
        <section className={ui.tarjeta}>
          <h2>Datos personales</h2>
          <div className={ui.grilla}>
            <label className={ui.campo}><span className={ui.etiqueta}>Nombre</span>
              <input className={ui.input} maxLength={60} value={datos.nombre} onChange={(e) => setDatos({ ...datos, nombre: e.target.value })} />{err('nombre')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Apellido</span>
              <input className={ui.input} maxLength={60} value={datos.apellido} onChange={(e) => setDatos({ ...datos, apellido: e.target.value })} />{err('apellido')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Cédula (opcional)</span>
              <input className={ui.input} maxLength={20} value={datos.cedula} onChange={(e) => setDatos({ ...datos, cedula: e.target.value })} />{err('cedula')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Teléfono (opcional)</span>
              <input className={ui.input} maxLength={30} value={datos.telefono} onChange={(e) => setDatos({ ...datos, telefono: e.target.value })} />{err('telefono')}</label>
          </div>
          <dl className={ui.datos} style={{ marginTop: 16 }}>
            <dt>Rol</dt><dd>{nombreDelRol(cuenta.rol)}</dd>
          </dl>
          <div className={ui.acciones}><button type="submit" className={ui.boton}>Guardar datos</button></div>
        </section>
      </form>

      <form onSubmit={(e) => enviar(e, () => api.put<UsuarioDto>('/api/admin/cuenta/email', email), 'Email actualizado. Úselo para iniciar sesión.')} noValidate>
        <section className={ui.tarjeta}>
          <h2>Email de acceso</h2>
          <div className={ui.grilla}>
            <label className={ui.campo}><span className={ui.etiqueta}>Email</span>
              <input className={ui.input} type="email" maxLength={120} value={email.email} onChange={(e) => setEmail({ ...email, email: e.target.value })} />{err('email')}</label>
            <label className={ui.campo}><span className={ui.etiqueta}>Contraseña actual (para confirmar)</span>
              <input className={ui.input} type="password" autoComplete="current-password" value={email.passwordActual} onChange={(e) => setEmail({ ...email, passwordActual: e.target.value })} />{err('passwordActual')}</label>
          </div>
          <div className={ui.acciones}><button type="submit" className={ui.botonSecundario}>Cambiar email</button></div>
        </section>
      </form>
    </>
  )
}

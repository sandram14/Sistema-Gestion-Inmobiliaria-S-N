import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { api } from '../api/cliente'
import type { UsuarioDto } from '../api/tipos'
import { useSesion } from '../auth/Sesion'
import { erroresDe } from '../componentes/errores'
import { nombreDelRol, textoFecha } from '../componentes/formato'
import ui from '../ui.module.css'

/** Usuarios del panel. ADMIN gestiona; GERENTE solo consulta. */
export default function Usuarios() {
  const { puede } = useSesion()
  const gestiona = puede('USUARIOS_GESTIONAR')
  const [usuarios, setUsuarios] = useState<UsuarioDto[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [texto, setTexto] = useState('')
  const [rol, setRol] = useState('')
  const [estado, setEstado] = useState('')

  useEffect(() => {
    api.get<UsuarioDto[]>('/api/admin/usuarios').then(setUsuarios).catch((e) => setError(erroresDe(e).general))
  }, [])

  const visibles = useMemo(() => {
    const t = texto.trim().toLowerCase()
    return (usuarios ?? []).filter((u) =>
      (!t || `${u.nombre} ${u.apellido} ${u.email}`.toLowerCase().includes(t)) &&
      (!rol || u.rol === rol) &&
      (!estado || (estado === 'activo') === u.activo))
  }, [usuarios, texto, rol, estado])

  return (
    <>
      <div className={ui.tituloPagina}>
        <h1>Usuarios</h1>
        {gestiona && <Link to="/usuarios/nuevo" className={ui.boton}><Plus size={16} aria-hidden="true" /> Nuevo usuario</Link>}
      </div>
      {error && <div className={ui.avisoError} role="alert">{error}</div>}

      <div className={ui.filtros}>
        <input className={ui.input} placeholder="Buscar por nombre o email" value={texto} onChange={(e) => setTexto(e.target.value)} aria-label="Buscar" />
        <select className={ui.input} value={rol} onChange={(e) => setRol(e.target.value)} aria-label="Rol">
          <option value="">Todos los roles</option>
          <option value="ADMIN">Administrador</option>
          <option value="GERENTE">Gerente</option>
          <option value="VENDEDOR">Vendedor</option>
        </select>
        <select className={ui.input} value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Estado">
          <option value="">Todos los estados</option>
          <option value="activo">Activos</option>
          <option value="inactivo">Desactivados</option>
        </select>
      </div>

      {usuarios === null && !error ? <p className={ui.cargando}>Cargando usuarios…</p> : (
        <div className={ui.tablaContenedor}>
          <table className={ui.tabla}>
            <thead><tr><th>Nombre</th><th>Email</th><th>Rol</th><th>Estado</th><th className={ui.numero}>Propiedades</th><th>Alta</th>{gestiona && <th></th>}</tr></thead>
            <tbody>
              {visibles.map((u) => (
                <tr key={u.id}>
                  <td>{u.nombre} {u.apellido}{u.debeCambiarPassword && <div className={ui.secundario}>Contraseña temporal pendiente</div>}</td>
                  <td>{u.email}</td>
                  <td>{nombreDelRol(u.rol)}</td>
                  <td><span className={`${ui.estado} ${u.activo ? '' : ui.estadoInactivo}`}>{u.activo ? 'Activo' : 'Desactivado'}</span></td>
                  <td className={ui.numero}>{u.propiedadesAsignadas}</td>
                  <td className={ui.secundario}>{textoFecha(u.creadoEn)}</td>
                  {gestiona && <td><Link to={`/usuarios/${u.id}`} className={`${ui.botonSecundario} ${ui.botonChico}`}>Editar</Link></td>}
                </tr>
              ))}
              {usuarios && visibles.length === 0 && <tr><td colSpan={7} className={ui.vacio}>No hay usuarios que coincidan con la búsqueda.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
      {usuarios && <p className={ui.pieTabla}>{visibles.length} de {usuarios.length} usuarios</p>}
    </>
  )
}

import { lazy } from 'react'
import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import RutaProtegida from './auth/RutaProtegida'
import Layout from './componentes/Layout'
import Login from './paginas/Login'

// Cada pantalla se descarga recién cuando se abre (carga diferida): el archivo inicial del panel
// queda chico aunque se agreguen secciones. Mientras carga, Layout muestra "Cargando…".
const ListadoPropiedades = lazy(() => import('./paginas/ListadoPropiedades'))
const FormularioPropiedad = lazy(() => import('./paginas/FormularioPropiedad'))
const MiCuenta = lazy(() => import('./paginas/MiCuenta'))
const CambiarContrasena = lazy(() => import('./paginas/CambiarContrasena'))
const Usuarios = lazy(() => import('./paginas/Usuarios'))
const FormularioUsuario = lazy(() => import('./paginas/FormularioUsuario'))
const ConsultasHistorial = lazy(() => import('./paginas/ConsultasHistorial'))
const ConsultasAnalisis = lazy(() => import('./paginas/ConsultasAnalisis'))
const DetalleConsulta = lazy(() => import('./paginas/DetalleConsulta'))
const ConsultasCalidad = lazy(() => import('./paginas/ConsultasCalidad'))
const ClientesPotenciales = lazy(() => import('./paginas/ClientesPotenciales'))
const DetalleClientePotencial = lazy(() => import('./paginas/DetalleClientePotencial'))
const NuevoClientePotencial = lazy(() => import('./paginas/NuevoClientePotencial'))

// Rutas: /admin/login · /admin · /admin/propiedades · /admin/propiedades/nueva · /admin/propiedades/:id · /admin/cuenta
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<RutaProtegida />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/propiedades" replace />} />
          <Route path="/propiedades" element={<ListadoPropiedades />} />
          <Route path="/propiedades/nueva" element={<FormularioConClave />} />
          <Route path="/propiedades/:id" element={<FormularioConClave />} />
          <Route path="/cuenta" element={<MiCuenta />} />
          <Route path="/cuenta/contrasena" element={<CambiarContrasena />} />
          <Route path="/consultas" element={<ConsultasHistorial />} />
          <Route path="/consultas/analisis" element={<ConsultasAnalisis />} />
          <Route path="/consultas/calidad" element={<ConsultasCalidad />} />
          <Route path="/consultas/clientes" element={<ClientesPotenciales />} />
          <Route path="/consultas/clientes/nuevo" element={<NuevoClientePotencial />} />
          <Route path="/consultas/clientes/:id" element={<DetalleClientePotencial />} />
          <Route path="/consultas/:id" element={<DetalleConsulta />} />
          <Route path="/usuarios" element={<Usuarios />} />
          <Route path="/usuarios/nuevo" element={<UsuarioConClave />} />
          <Route path="/usuarios/:id" element={<UsuarioConClave />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

/**
 * El formulario se vuelve a crear al cambiar de propiedad (por ejemplo, de "nueva" a la
 * edición recién creada), así no conserva datos ni mensajes de la pantalla anterior.
 */
function FormularioConClave() {
  const { id } = useParams()
  return <FormularioPropiedad key={id ?? 'nueva'} />
}

function UsuarioConClave() {
  const { id } = useParams()
  return <FormularioUsuario key={id ?? 'nuevo'} />
}

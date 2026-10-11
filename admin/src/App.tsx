import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import RutaProtegida from './auth/RutaProtegida'
import Layout from './componentes/Layout'
import Login from './paginas/Login'
import ListadoPropiedades from './paginas/ListadoPropiedades'
import FormularioPropiedad from './paginas/FormularioPropiedad'
import MiCuenta from './paginas/MiCuenta'

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

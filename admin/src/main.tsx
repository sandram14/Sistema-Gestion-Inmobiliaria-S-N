import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { ProveedorSesion } from './auth/Sesion'
import 'leaflet/dist/leaflet.css'
import './global.css'
import { aplicarTema, temaGuardado } from './componentes/tema'

// El tema elegido se aplica antes de mostrar el panel (evita un destello del tema claro)
aplicarTema(temaGuardado())

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    {/* Todas las rutas del panel cuelgan de /admin/ */}
    <BrowserRouter basename="/admin">
      <ProveedorSesion>
        <App />
      </ProveedorSesion>
    </BrowserRouter>
  </React.StrictMode>
)

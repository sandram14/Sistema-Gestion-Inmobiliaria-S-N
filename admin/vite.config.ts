import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Panel administrativo: aplicación independiente del sitio público.
// Se sirve bajo /admin/ (en producción, nginx la publica en ese mismo dominio, así la
// cookie de sesión del backend funciona). En desarrollo, /api y /uploads van a Tomcat.
const BACKEND = process.env.VITE_BACKEND_URL || 'http://localhost:8080'
// Las fotos originales (/images/...) las publica el sitio público; en desarrollo se piden
// a su servidor (npm run dev en frontend/). En producción, nginx sirve todo en un dominio.
const FRONTEND = process.env.VITE_FRONTEND_URL || 'http://localhost:5173'
const proxyBackend = {
  '/api': { target: BACKEND, changeOrigin: false },
  '/uploads': { target: BACKEND, changeOrigin: false },
  '/images': { target: FRONTEND, changeOrigin: false },
}

export default defineConfig({
  base: '/admin/',
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: parseInt(process.env.PORT || '5174'),
    strictPort: true,
    proxy: proxyBackend,
  },
  preview: {
    host: '0.0.0.0',
    port: parseInt(process.env.PORT || '4174'),
    proxy: proxyBackend,
  },
})

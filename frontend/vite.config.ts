import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// Backend Java (Tomcat). En desarrollo, /api y /uploads se reenvían allí, así el navegador
// ve un único origen (sin CORS) y la cookie del asistente funciona.
const BACKEND = process.env.VITE_BACKEND_URL || 'http://localhost:8080'
const proxyBackend = {
  '/api': { target: BACKEND, changeOrigin: false },
  '/uploads': { target: BACKEND, changeOrigin: false },
}

// Vite config — https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: parseInt(process.env.PORT || '5173'),
    proxy: proxyBackend,
  },
  preview: {
    host: '0.0.0.0',
    port: parseInt(process.env.PORT || '4173'),
    proxy: proxyBackend,
  },
})

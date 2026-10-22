import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'node:path'
// Pruebas de interfaz con un backend simulado (npm test). Ver docs/pruebas.md
export default defineConfig({ plugins: [react()], test: { environment: 'jsdom', include: ['verificacion/**/*.test.tsx'], root: path.resolve(import.meta.dirname, '..') } })

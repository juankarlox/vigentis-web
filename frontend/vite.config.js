import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, las llamadas a /api se envían al backend de Spring Boot (puerto 8080)
export default defineConfig({
  plugins: [react()],
  // "npm run build" deja la web dentro del backend: así Vigentis es una sola aplicación
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})

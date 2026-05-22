import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // proxy: em dev, encaminha as chamadas /api para o backend Spring Boot.
  // Assim o frontend usa caminho relativo e não precisa de CORS.
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})

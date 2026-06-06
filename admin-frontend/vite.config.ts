import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: mode === 'production' ? '/admin/' : '/',
  server: {
    port: 5173,
    proxy: {
      '/api/admin': {
        target: 'https://localhost:8443',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static/admin',
    emptyOutDir: true
  }
}))

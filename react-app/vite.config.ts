import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // Equivalente ao --host 0.0.0.0
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // Equivalente ao proxy.conf.json
      '/api': {
        target: 'http://localhost:8080',
        secure: false,
        changeOrigin: true,
        // Opcional: remove o prefixo /api antes de enviar ao Spring Boot
        // rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
});

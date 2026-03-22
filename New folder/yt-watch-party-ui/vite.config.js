import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // FIX: Tell Vite that 'global' means 'window' in the browser
  define: {
    global: 'window',
  },
})
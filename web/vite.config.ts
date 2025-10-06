import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  root: path.resolve(__dirname, 'src'),
  build: {
    outDir: path.resolve(__dirname, '../build/web')
  },
  server: {
    port: 5173
  }
})

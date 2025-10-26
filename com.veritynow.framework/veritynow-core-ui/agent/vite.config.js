import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from "path";

export default defineConfig({
  plugins: [react(), tailwind()],
  resolve: {
      alias: {
        "@": path.resolve(__dirname, "src")
      }
    },

  base: '/app/',
  build: { outDir: 'dist', emptyOutDir: true },
  server: {
    port: 5172,
    proxy: { '/api': 'http://localhost:8080', '/ui' : 'http://localhost:8080', '/images' : 'http://localhost:8080'}
  }
})

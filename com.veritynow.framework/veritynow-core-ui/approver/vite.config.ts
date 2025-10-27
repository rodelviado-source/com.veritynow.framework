import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'; 
import eslint from 'vite-plugin-eslint'; 
import path from 'node:path'


export default defineConfig({
	
		plugins: [react(), tailwindcss(), 	eslint({
	      // Optional: configure ESLint options here
	      cache: false, // Disable caching for development, enable for production
	      failOnWarning: false, // Don't fail the build on warnings
	      failOnError: true, // Fail the build on errors
	    })],
		resolve: {
			alias: {
				'@': path.resolve(__dirname, './src'),
				'@components': path.resolve(__dirname, './src/components'),
				'@lib': path.resolve(__dirname, './src/lib'),

			}
		},
		server: {
		  //port : 5173	
		  proxy: { "/api": { target: "http://127.0.0.1:8080", changeOrigin: true } },
		},

		
	})
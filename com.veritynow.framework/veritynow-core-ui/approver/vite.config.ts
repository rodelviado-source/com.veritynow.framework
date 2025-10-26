import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'; 
import eslint from 'vite-plugin-eslint'; 
import path from 'node:path'
import { fileURLToPath } from 'node:url';


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
				"@": fileURLToPath(new URL("./src", import.meta.url)) 
			}
			
		},
		server: {
		    port: 5172,
		    proxy: { 
				// string shorthand: /foo -> http://localhost:4567/foo
				   '/foo': 'http://localhost:4567',
				   // with options
				   '/api': {
				     target: 'http://localhost:8080',
				     changeOrigin: true,
				     rewrite: path => path.replace(/^\/api/, '')
				
			//	'/api': 'http://localhost:8080', '/ui' : 'http://localhost:8080', '/images' : 'http://localhost:8080'
			 }
		 }
	}
)
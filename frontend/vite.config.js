import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    rolldownOptions: {
      output: {
        // The module screens and the languages other than English are split by dynamic import
        // (shared/utils/lazyScreen.js, shared/i18n/resources.js). React itself gets a chunk of its
        // own: it changes far less often than the application, so browsers keep it cached.
        codeSplitting: {
          groups: [{ name: 'react', test: /node_modules[\\/](react|react-dom|scheduler)[\\/]/ }],
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    setupFiles: ['./src/test/setup.js'],
    // The suite runs slowly when the machine is busy: leave room before timing out.
    testTimeout: 15000,
  },
})   

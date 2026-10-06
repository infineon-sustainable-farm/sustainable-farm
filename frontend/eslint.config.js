import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      // Les modules declarent un adaptateur d'ecran (withModuleUi) reutilise par le
      // routeur : c'est un HOC, pas un composant, et la regle le signale par defaut
      // dans ModuleScreen.jsx. extraHOCs le declare explicitement plutot que de
      // desactiver la regle sur tout le projet.
      'react-refresh/only-export-components': ['error', { extraHOCs: ['withModuleUi'] }],
    },
  },
])

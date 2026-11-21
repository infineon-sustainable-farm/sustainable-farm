import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import i18next from 'eslint-plugin-i18next'
import { defineConfig, globalIgnores } from 'eslint/config'

// Interface text goes through i18next (t('...')) everywhere: hard-coded text in JSX is refused,
// except in tests and test fixtures and in the excluded module cropstorage, not migrated (ADR 0001).
const I18N_FILES = ['src/**/*.{js,jsx}']
const I18N_EXCLUDED = [
  '**/*.test.{js,jsx}',
  'src/test/**',
  'src/features/cropstorage/**',
]

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
  {
    // Node scripts run outside the browser.
    files: ['scripts/**/*.{js,mjs}'],
    languageOptions: { globals: globals.node },
  },
  {
    files: I18N_FILES,
    ignores: I18N_EXCLUDED,
    plugins: { i18next },
    rules: {
      // jsx-only: text between tags and string literals inside JSX expressions,
      // plus the attributes read by users and assistive technologies.
      'i18next/no-literal-string': [
        'error',
        {
          mode: 'jsx-only',
          'jsx-attributes': { include: ['aria-label', 'aria-description', 'title', 'alt', 'placeholder', 'label'] },
          message: 'Hard-coded text in JSX, use t() from react-i18next',
        },
      ],
    },
  },
])

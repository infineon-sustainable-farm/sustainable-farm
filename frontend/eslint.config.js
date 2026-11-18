import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import i18next from 'eslint-plugin-i18next'
import { defineConfig, globalIgnores } from 'eslint/config'

// Folders whose interface text goes through i18next (t('...')). Hard-coded text in
// their JSX is refused. Add a module here once it is migrated.
const I18N_MIGRATED_FILES = [
  'src/shared/ui/**/*.{js,jsx}',
  'src/app/**/*.{js,jsx}',
  'src/features/machinery/**/*.{js,jsx}',
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
    files: I18N_MIGRATED_FILES,
    ignores: ['**/*.test.{js,jsx}'],
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

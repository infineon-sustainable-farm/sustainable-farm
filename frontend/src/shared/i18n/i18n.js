import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import {
  DEFAULT_NAMESPACE,
  FALLBACK_LANGUAGE,
  LANGUAGE_STORAGE_KEY,
  SUPPORTED_LANGUAGES,
} from './config'
import { localeBackend, namespaces, resources } from './resources'

// Keeps <html lang> in line with the active language (screen readers,
// hyphenation, spell checking).
function syncDocumentLanguage(language) {
  if (typeof document === 'undefined' || !language) return
  document.documentElement.lang = language
}

i18n.on('languageChanged', () => syncDocumentLanguage(i18n.resolvedLanguage))

if (!i18n.isInitialized) {
  i18n
    .use(LanguageDetector)
    .use(localeBackend)
    .use(initReactI18next)
    .init({
      // English is bundled, the other languages load on demand (resources.js).
      resources,
      partialBundledLanguages: true,
      ns: namespaces,
      defaultNS: DEFAULT_NAMESPACE,
      fallbackLng: FALLBACK_LANGUAGE,
      supportedLngs: SUPPORTED_LANGUAGES,
      // "de-CH" from the browser resolves to "de".
      nonExplicitSupportedLngs: true,
      load: 'languageOnly',
      // English is bundled, so initialisation can be synchronous; a language loaded on demand
      // suspends the screens (react-i18next useSuspense) until its files arrive.
      initAsync: false,
      interpolation: { escapeValue: false }, // React already escapes
      returnNull: false,
      detection: {
        order: ['localStorage', 'navigator'],
        lookupLocalStorage: LANGUAGE_STORAGE_KEY,
        caches: ['localStorage'],
      },
    })
}

syncDocumentLanguage(i18n.resolvedLanguage)

export default i18n

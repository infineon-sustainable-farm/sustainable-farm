// Languages the interface is translated into. English is the reference
// language: every other locale must have exactly the same keys
// (checked by scripts/check-i18n.mjs).
export const SUPPORTED_LANGUAGES = ['en', 'de', 'fr']
export const FALLBACK_LANGUAGE = 'en'
export const DEFAULT_NAMESPACE = 'common'

// localStorage key holding the language chosen by the user.
export const LANGUAGE_STORAGE_KEY = 'sf.lang'

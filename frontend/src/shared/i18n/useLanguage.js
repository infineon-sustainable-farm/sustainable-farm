import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { FALLBACK_LANGUAGE, SUPPORTED_LANGUAGES } from './config'

/**
 * Current interface language and a setter. The choice is persisted in
 * localStorage by the language detector and mirrored on <html lang>.
 *
 * @returns {{ language: string, setLanguage: (lng: string) => Promise<void>, supportedLanguages: string[] }}
 */
export function useLanguage() {
  const { i18n } = useTranslation()

  const setLanguage = useCallback(
    async (language) => {
      if (!SUPPORTED_LANGUAGES.includes(language)) {
        throw new Error(`Unsupported language "${language}", expected one of ${SUPPORTED_LANGUAGES.join(', ')}`)
      }
      await i18n.changeLanguage(language)
    },
    [i18n],
  )

  return {
    language: i18n.resolvedLanguage ?? FALLBACK_LANGUAGE,
    setLanguage,
    supportedLanguages: SUPPORTED_LANGUAGES,
  }
}

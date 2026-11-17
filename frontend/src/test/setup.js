import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import i18n from '../shared/i18n/i18n'
import { FALLBACK_LANGUAGE } from '../shared/i18n/config'

// Components render with real translations; every test starts in English.
await i18n.changeLanguage(FALLBACK_LANGUAGE)

// DOM cleanup between tests (when globals are not active on the RTL side).
afterEach(() => {
  cleanup()
})

// Simulated localStorage for jsdom (already provided, but we guarantee a clean state).
// Files on the default node environment have no localStorage.
afterEach(async () => {
  if (i18n.resolvedLanguage !== FALLBACK_LANGUAGE) await i18n.changeLanguage(FALLBACK_LANGUAGE)
  if (typeof localStorage !== 'undefined') localStorage.clear()
  vi.restoreAllMocks()
})

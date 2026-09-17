import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// Nettoyage du DOM entre les tests (lorsque globals n'est pas actif côté RTL).
afterEach(() => {
  cleanup()
})

// localStorage simulé pour jsdom (déjà fourni, mais on garantit un état propre).
afterEach(() => {
  localStorage.clear()
  vi.restoreAllMocks()
})
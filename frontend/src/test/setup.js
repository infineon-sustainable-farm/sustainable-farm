import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// DOM cleanup between tests (when globals are not active on the RTL side).
afterEach(() => {
  cleanup()
})

// Simulated localStorage for jsdom (already provided, but we guarantee a clean state).
// Files on the default node environment have no localStorage.
afterEach(() => {
  if (typeof localStorage !== 'undefined') localStorage.clear()
  vi.restoreAllMocks()
})
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'

// DOM cleanup between tests (when globals are not active on the RTL side).
afterEach(() => {
  cleanup()
})

// Simulated localStorage for jsdom (already provided, but we guarantee a clean state).
afterEach(() => {
  localStorage.clear()
  vi.restoreAllMocks()
})
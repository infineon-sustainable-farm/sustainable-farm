// @vitest-environment jsdom
import { act, render, renderHook, screen } from '@testing-library/react'
import { useTranslation } from 'react-i18next'
import { describe, expect, it } from 'vitest'
import { LANGUAGE_STORAGE_KEY, SUPPORTED_LANGUAGES } from './config'
import { useLanguage } from './useLanguage'

function AppName() {
  const { t } = useTranslation('machinery')
  return <p>{t('name')}</p>
}

describe('useLanguage', () => {
  it('starts in English and exposes the supported languages', () => {
    const { result } = renderHook(() => useLanguage())
    expect(result.current.language).toBe('en')
    expect(result.current.supportedLanguages).toEqual(SUPPORTED_LANGUAGES)
    expect(document.documentElement.lang).toBe('en')
  })

  it('switches language, persists it and updates <html lang> and the texts', async () => {
    const { result } = renderHook(() => useLanguage())
    render(<AppName />)
    expect(screen.getByText('Machinery')).toBeInTheDocument()

    await act(() => result.current.setLanguage('fr'))

    expect(result.current.language).toBe('fr')
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('fr')
    expect(document.documentElement.lang).toBe('fr')
    expect(screen.getByText('Machines')).toBeInTheDocument()

    await act(() => result.current.setLanguage('de'))
    expect(screen.getByText('Maschinen')).toBeInTheDocument()
  })

  it('refuses an unsupported language', async () => {
    const { result } = renderHook(() => useLanguage())
    await expect(result.current.setLanguage('es')).rejects.toThrow('Unsupported language "es"')
    expect(result.current.language).toBe('en')
  })
})

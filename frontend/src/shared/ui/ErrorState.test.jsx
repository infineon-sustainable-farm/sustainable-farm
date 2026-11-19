// @vitest-environment jsdom
import { act, fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import ErrorState from './ErrorState'

describe('ErrorState', () => {
  it('announces the error with an icon and text', () => {
    render(<ErrorState message="Failed to load the dashboard." />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Failed to load the dashboard.')
    expect(alert).toHaveClass('text-error-fg')
    expect(alert.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('retries with a secondary button', () => {
    const onRetry = vi.fn()
    render(<ErrorState onRetry={onRetry} />)
    const button = screen.getByRole('button', { name: 'Retry' })
    expect(button).toHaveClass('border-border-input')
    fireEvent.click(button)
    expect(onRetry).toHaveBeenCalled()
  })

  it('shows the retry as busy while it runs', () => {
    render(<ErrorState onRetry={() => {}} retrying />)
    expect(screen.getByRole('button', { name: 'Retry' })).toHaveAttribute('aria-busy', 'true')
  })

  it('translates its default texts', async () => {
    await act(() => i18n.changeLanguage('fr'))
    render(<ErrorState onRetry={() => {}} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Une erreur est survenue. Réessayez.')
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument()
  })
})

// @vitest-environment jsdom
import { act, fireEvent, render, screen } from '@testing-library/react'
import { Inbox } from 'lucide-react'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import EmptyState from './EmptyState'

describe('EmptyState', () => {
  it('shows its message with the machinery look', () => {
    const { container } = render(<EmptyState message="No equipment registered yet." />)
    expect(screen.getByText('No equipment registered yet.')).toBeInTheDocument()
    expect(container.firstChild).toHaveClass('py-16', 'text-center', 'text-text-muted')
  })

  it('renders a title, a decorative icon and a primary action', () => {
    const onAction = vi.fn()
    const { container } = render(
      <EmptyState title="Nothing here" message="Add the first one." icon={Inbox} actionLabel="Add equipment" onAction={onAction} />,
    )
    expect(screen.getByText('Nothing here')).toBeInTheDocument()
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
    fireEvent.click(screen.getByRole('button', { name: 'Add equipment' }))
    expect(onAction).toHaveBeenCalled()
  })

  it('falls back to the translated "no results" text', async () => {
    render(<EmptyState />)
    expect(screen.getByText('No results')).toBeInTheDocument()
    await act(() => i18n.changeLanguage('de'))
    expect(screen.getByText('Keine Ergebnisse')).toBeInTheDocument()
  })
})

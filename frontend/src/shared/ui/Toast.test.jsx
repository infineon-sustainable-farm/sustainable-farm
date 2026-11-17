// @vitest-environment jsdom
import { act, fireEvent, render, renderHook, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import { ToastProvider } from './Toast'
import { TOAST_DURATION, useToast } from './toastContext'

function Trigger({ tone = 'success', duration }) {
  const toast = useToast()
  return (
    <button type="button" onClick={() => toast.show({ message: 'Equipment saved', tone, duration })}>
      Notify
    </button>
  )
}

function renderWithProvider(ui) {
  return render(<ToastProvider>{ui}</ToastProvider>)
}

describe('Toast', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('announces toasts in a polite live region', () => {
    renderWithProvider(<Trigger />)
    const region = screen.getByRole('region', { name: 'Notifications' })
    const list = region.querySelector('[aria-live]')
    expect(list).toHaveAttribute('aria-live', 'polite')
    fireEvent.click(screen.getByRole('button', { name: 'Notify' }))
    expect(within(region).getByText('Equipment saved')).toBeInTheDocument()
  })

  it('shows the icon of its tone, not colour alone', () => {
    renderWithProvider(<Trigger tone="error" />)
    fireEvent.click(screen.getByRole('button', { name: 'Notify' }))
    const item = screen.getByText('Equipment saved').closest('li')
    expect(item.querySelector('svg')).toHaveClass('text-error-fg')
    expect(item.className).toContain('motion-reduce:transition-none')
  })

  it('closes on its own after the duration', () => {
    renderWithProvider(<Trigger />)
    fireEvent.click(screen.getByRole('button', { name: 'Notify' }))
    act(() => vi.advanceTimersByTime(TOAST_DURATION - 1))
    expect(screen.getByText('Equipment saved')).toBeInTheDocument()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByText('Equipment saved')).not.toBeInTheDocument()
  })

  it('pauses while hovered', () => {
    renderWithProvider(<Trigger />)
    fireEvent.click(screen.getByRole('button', { name: 'Notify' }))
    const item = screen.getByText('Equipment saved').closest('li')
    fireEvent.mouseEnter(item)
    act(() => vi.advanceTimersByTime(TOAST_DURATION * 2))
    expect(screen.getByText('Equipment saved')).toBeInTheDocument()
    fireEvent.mouseLeave(item)
    act(() => vi.advanceTimersByTime(TOAST_DURATION))
    expect(screen.queryByText('Equipment saved')).not.toBeInTheDocument()
  })

  it('stays with duration 0 and closes with its button', () => {
    renderWithProvider(<Trigger duration={0} />)
    fireEvent.click(screen.getByRole('button', { name: 'Notify' }))
    act(() => vi.advanceTimersByTime(TOAST_DURATION * 3))
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))
    expect(screen.queryByText('Equipment saved')).not.toBeInTheDocument()
  })

  it('has success and error shortcuts', () => {
    const { result } = renderHook(() => useToast(), { wrapper: ToastProvider })
    act(() => {
      result.current.success('Saved')
      result.current.error('Failed')
    })
    expect(screen.getByText('Saved')).toBeInTheDocument()
    expect(screen.getByText('Failed')).toBeInTheDocument()
  })

  it('refuses to work without a provider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => renderHook(() => useToast())).toThrow(/ToastProvider/)
  })

  it('translates its labels', async () => {
    await act(() => i18n.changeLanguage('fr'))
    renderWithProvider(<Trigger duration={0} />)
    expect(screen.getByRole('region', { name: 'Notifications' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Notify' }))
    expect(screen.getByRole('button', { name: 'Masquer' })).toBeInTheDocument()
  })
})

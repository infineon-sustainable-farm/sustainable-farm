// @vitest-environment jsdom
import { act, fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import Pagination from './Pagination'

describe('Pagination', () => {
  it('is a named navigation with the current page and two buttons', () => {
    render(<Pagination page={1} totalPages={4} onPageChange={() => {}} />)
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 4')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('keeps the button labels on one line and wraps the row on narrow screens', () => {
    render(<Pagination page={1} totalPages={4} onPageChange={() => {}} />)
    expect(screen.getByRole('navigation')).toHaveClass('flex-wrap')
    expect(screen.getByRole('button', { name: 'Previous' })).toHaveClass('whitespace-nowrap')
  })

  it('moves to the previous and next pages', () => {
    const onPageChange = vi.fn()
    render(<Pagination page={1} totalPages={4} onPageChange={onPageChange} />)
    fireEvent.click(screen.getByRole('button', { name: 'Previous' }))
    expect(onPageChange).toHaveBeenLastCalledWith(0)
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(onPageChange).toHaveBeenLastCalledWith(2)
  })

  it('disables Previous on the first page and Next on the last', () => {
    const { rerender } = render(<Pagination page={0} totalPages={2} onPageChange={() => {}} />)
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    rerender(<Pagination page={1} totalPages={2} onPageChange={() => {}} />)
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('renders nothing without pages', () => {
    const { container } = render(<Pagination page={0} totalPages={0} onPageChange={() => {}} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('translates its texts', async () => {
    await act(() => i18n.changeLanguage('de'))
    render(<Pagination page={0} totalPages={3} onPageChange={() => {}} />)
    expect(screen.getByRole('navigation', { name: 'Seitennavigation' })).toBeInTheDocument()
    expect(screen.getByText('Seite 1 von 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Zurück' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Weiter' })).toBeInTheDocument()
  })
})

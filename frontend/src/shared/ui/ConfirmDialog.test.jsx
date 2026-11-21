// @vitest-environment jsdom
import { act, fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import ConfirmDialog from './ConfirmDialog'

describe('ConfirmDialog', () => {
  it('asks with the default title and buttons, focus on Cancel', () => {
    render(<ConfirmDialog open onClose={() => {}} onConfirm={() => {}} message="Tractor 1 will be deleted." />)
    const dialog = screen.getByRole('dialog', { name: 'Delete this item?' })
    expect(dialog).toHaveAccessibleDescription('Tractor 1 will be deleted.')
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus()
    expect(screen.getByRole('button', { name: 'Delete' })).toHaveClass('bg-danger')
  })

  it('confirms and cancels', () => {
    const onClose = vi.fn()
    const onConfirm = vi.fn()
    render(<ConfirmDialog open onClose={onClose} onConfirm={onConfirm} title="Delete equipment" message="Sure?" />)
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
    expect(onConfirm).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('blocks every way out while the request runs', () => {
    const onClose = vi.fn()
    render(<ConfirmDialog open onClose={onClose} onConfirm={() => {}} message="Sure?" loading />)
    expect(screen.getByRole('button', { name: 'Delete' })).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).not.toHaveBeenCalled()
  })

  it('shows a failed request as an alert with an icon', () => {
    render(<ConfirmDialog open onClose={() => {}} onConfirm={() => {}} message="Sure?" error="Equipment is in use." />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Equipment is in use.')
    expect(alert.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })

  it('uses custom labels', () => {
    render(
      <ConfirmDialog open onClose={() => {}} onConfirm={() => {}} message="m" confirmLabel="Archive" cancelLabel="Keep" variant="primary" />,
    )
    expect(screen.getByRole('button', { name: 'Archive' })).toHaveClass('bg-primary')
    expect(screen.getByRole('button', { name: 'Keep' })).toBeInTheDocument()
  })

  it('translates its default texts', async () => {
    await act(() => i18n.changeLanguage('de'))
    render(<ConfirmDialog open onClose={() => {}} onConfirm={() => {}} message="m" />)
    expect(screen.getByRole('dialog', { name: 'Diesen Eintrag löschen?' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Abbrechen' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Löschen' })).toBeInTheDocument()
  })
})

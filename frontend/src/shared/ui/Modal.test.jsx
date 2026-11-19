// @vitest-environment jsdom
import { act, fireEvent, render, screen } from '@testing-library/react'
import { useRef, useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import i18n from '../i18n/i18n'
import Modal from './Modal'

function Harness({ onClose = () => {}, withInitialFocus = false, dismissible = true }) {
  const [open, setOpen] = useState(false)
  const nameRef = useRef(null)
  function close() {
    onClose()
    setOpen(false)
  }
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Open
      </button>
      <Modal
        open={open}
        onClose={close}
        title="Add equipment"
        description="Fill in the asset."
        initialFocusRef={withInitialFocus ? nameRef : undefined}
        dismissible={dismissible}
        footer={<button type="submit">Save</button>}
      >
        <input aria-label="Name" />
        <input aria-label="Code" ref={nameRef} />
      </Modal>
    </>
  )
}

function openModal() {
  const trigger = screen.getByRole('button', { name: 'Open' })
  trigger.focus()
  fireEvent.click(trigger)
  return trigger
}

describe('Modal', () => {
  it('renders nothing when closed', () => {
    render(<Modal open={false} onClose={() => {}} title="T" />)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('is a modal dialog named by its title and described by its description', () => {
    render(<Harness />)
    openModal()
    const dialog = screen.getByRole('dialog', { name: 'Add equipment' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog).toHaveAccessibleDescription('Fill in the asset.')
    expect(dialog).toHaveClass('rounded-control', 'bg-surface-raised', 'p-6', 'max-w-md')
    expect(dialog.parentElement).toHaveClass('bg-backdrop', 'z-(--z-overlay)')
  })

  it('animates its entrance in 300 ms with the tokens, and not under reduced motion', () => {
    render(<Harness />)
    openModal()
    const dialog = screen.getByRole('dialog')
    expect(dialog.className).toContain('duration-(--duration-slow)')
    expect(dialog.className).toContain('motion-reduce:transition-none')
  })

  it('focuses the first field on open, or the element given', () => {
    const { unmount } = render(<Harness />)
    openModal()
    expect(screen.getByRole('textbox', { name: 'Name' })).toHaveFocus()
    unmount()
    render(<Harness withInitialFocus />)
    openModal()
    expect(screen.getByRole('textbox', { name: 'Code' })).toHaveFocus()
  })

  it('traps Tab and Shift+Tab inside the dialog', () => {
    render(<Harness />)
    openModal()
    const close = screen.getByRole('button', { name: 'Close' })
    const save = screen.getByRole('button', { name: 'Save' })
    save.focus()
    fireEvent.keyDown(document, { key: 'Tab' })
    expect(close).toHaveFocus()
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true })
    expect(save).toHaveFocus()
  })

  it('closes on Escape and gives the focus back to the trigger', () => {
    const onClose = vi.fn()
    render(<Harness onClose={onClose} />)
    const trigger = openModal()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  it('closes with the close button and on a backdrop click, not on a click inside', () => {
    const onClose = vi.fn()
    render(<Harness onClose={onClose} />)
    openModal()
    fireEvent.mouseDown(screen.getByRole('dialog'))
    expect(onClose).not.toHaveBeenCalled()
    fireEvent.mouseDown(screen.getByRole('dialog').parentElement)
    expect(onClose).toHaveBeenCalledTimes(1)
    openModal()
    fireEvent.click(screen.getByRole('button', { name: 'Close' }))
    expect(onClose).toHaveBeenCalledTimes(2)
  })

  it('cannot be dismissed while not dismissible', () => {
    const onClose = vi.fn()
    render(<Harness onClose={onClose} dismissible={false} />)
    openModal()
    fireEvent.keyDown(document, { key: 'Escape' })
    fireEvent.mouseDown(screen.getByRole('dialog').parentElement)
    expect(onClose).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Close' })).toBeDisabled()
  })

  it('locks the page scroll while open', () => {
    render(<Harness />)
    openModal()
    expect(document.body.style.overflow).toBe('hidden')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(document.body.style.overflow).toBe('')
  })

  it('translates the close button', async () => {
    await act(() => i18n.changeLanguage('fr'))
    render(<Harness />)
    openModal()
    expect(screen.getByRole('button', { name: 'Fermer' })).toBeInTheDocument()
  })
})

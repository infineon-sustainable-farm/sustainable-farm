// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { Pencil, Trash2 } from 'lucide-react'
import { describe, expect, it, vi } from 'vitest'
import IconButton from './IconButton'

describe('IconButton', () => {
  it('is named by its label and calls onClick', () => {
    const onClick = vi.fn()
    render(<IconButton icon={Pencil} label="Edit status" onClick={onClick} />)
    const button = screen.getByRole('button', { name: 'Edit status' })
    expect(button).toHaveAttribute('title', 'Edit status')
    fireEvent.click(button)
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('throws without a label', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<IconButton icon={Pencil} />)).toThrow(/label/)
  })

  it('has a 32 px target that grows to 44 px on touch screens, and the focus mark', () => {
    render(<IconButton icon={Trash2} label="Delete" tone="danger" />)
    const button = screen.getByRole('button', { name: 'Delete' })
    expect(button).toHaveClass('h-8', 'w-8', 'pointer-coarse:h-11', 'text-error-fg')
    expect(button.className).toContain('focus-visible:outline-2')
  })

  it('can be disabled', () => {
    const onClick = vi.fn()
    render(<IconButton icon={Trash2} label="Delete" disabled onClick={onClick} />)
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
    expect(onClick).not.toHaveBeenCalled()
  })
})

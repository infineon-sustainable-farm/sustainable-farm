// @vitest-environment jsdom
import { fireEvent, render, screen } from '@testing-library/react'
import { createRef } from 'react'
import { describe, expect, it, vi } from 'vitest'
import Input from './Input'

describe('Input', () => {
  it('renders a text input with the machinery field look and the keyboard focus mark', () => {
    render(<Input aria-label="Name" />)
    const input = screen.getByRole('textbox', { name: 'Name' })
    expect(input).toHaveAttribute('type', 'text')
    expect(input).toHaveClass('border-border-input', 'rounded-control', 'px-3', 'py-2', 'w-full')
    expect(input.className).toContain('focus-visible:outline-2')
    expect(input.className).not.toMatch(/(^|\s)focus:ring/)
  })

  it('forwards value, onChange and ref', () => {
    const onChange = vi.fn()
    const ref = createRef()
    render(<Input aria-label="Name" value="" onChange={onChange} ref={ref} />)
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Tractor' } })
    expect(onChange).toHaveBeenCalled()
    expect(ref.current).toBe(screen.getByRole('textbox'))
  })

  it('works on its own with aria-invalid and disabled', () => {
    render(<Input aria-label="Name" aria-invalid="true" disabled />)
    const input = screen.getByRole('textbox')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toBeDisabled()
  })
})

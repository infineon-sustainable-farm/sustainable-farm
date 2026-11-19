// @vitest-environment jsdom
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Field from './Field'
import Input from './Input'
import Select from './Select'

describe('Field', () => {
  it('labels its control', () => {
    render(
      <Field label="Name">
        <Input />
      </Field>,
    )
    expect(screen.getByLabelText('Name')).toBe(screen.getByRole('textbox'))
  })

  it('links the hint with aria-describedby', () => {
    render(
      <Field label="Name" hint="30 characters at most">
        <Input />
      </Field>,
    )
    const input = screen.getByRole('textbox', { name: 'Name' })
    expect(input).toHaveAccessibleDescription('30 characters at most')
    expect(input).not.toHaveAttribute('aria-invalid')
    expect(screen.getByText('30 characters at most')).toHaveClass('text-text-muted')
  })

  it('shows the error with an icon and text, marks the control invalid and replaces the hint', () => {
    const { container } = render(
      <Field label="Name" hint="30 characters at most" error="Name is required.">
        <Input />
      </Field>,
    )
    const input = screen.getByRole('textbox', { name: 'Name' })
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('Name is required.')
    expect(screen.queryByText('30 characters at most')).not.toBeInTheDocument()
    const error = screen.getByText('Name is required.').parentElement
    expect(error).toHaveClass('text-error-fg')
    expect(error.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
    expect(container.querySelector('svg')).not.toBeNull()
  })

  it('keeps an id given by the caller and passes required and disabled', () => {
    render(
      <Field label="Stage" id="stage" required disabled>
        <Select options={[{ value: 'a', label: 'A' }]} />
      </Field>,
    )
    const select = screen.getByRole('combobox', { name: 'Stage' })
    expect(select).toHaveAttribute('id', 'stage')
    expect(select).toBeRequired()
    expect(select).toBeDisabled()
  })
})
